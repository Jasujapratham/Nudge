package com.nudge.service;

import com.nudge.dto.ReminderRequest;
import com.nudge.dto.ReminderResponse;
import com.nudge.entity.Priority;
import com.nudge.entity.Reminder;
import com.nudge.entity.User;
import com.nudge.exception.InvalidReminderTimeException;
import com.nudge.exception.ResourceNotFoundException;
import com.nudge.repository.ReminderRepository;
import com.nudge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * All reminder operations live here.
 *
 * <p>Every method takes the id of the currently logged-in user and re-reads the
 * row through {@code findByIdAndUserId}. That single pattern is what guarantees
 * a user can never read, edit or delete somebody else's reminder - a controller
 * that used {@code findById(id)} would leak data across accounts.</p>
 */
@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserRepository userRepository;

    public ReminderService(ReminderRepository reminderRepository, UserRepository userRepository) {
        this.reminderRepository = reminderRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ReminderResponse> findAllForUser(Long userId) {
        return reminderRepository.findByUserIdOrderByDateAscTimeAsc(userId).stream()
                .map(ReminderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReminderResponse findByIdForUser(Long userId, Long reminderId) {
        return ReminderResponse.from(loadOwnedReminder(userId, reminderId));
    }

    @Transactional
    public ReminderResponse create(Long userId, ReminderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // A reminder in the past can never ring, so reject it up front.
        requireFutureDateTime(request.getDate(), request.getTime());

        Reminder reminder = new Reminder();
        reminder.setUser(user);
        applyRequest(reminder, request);
        reminder.setCompleted(false);

        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @Transactional
    public ReminderResponse update(Long userId, Long reminderId, ReminderRequest request) {
        Reminder reminder = loadOwnedReminder(userId, reminderId);
        applyRequest(reminder, request);

        if (request.getCompleted() != null) {
            reminder.setCompleted(request.getCompleted());
        }

        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    /**
     * Flips the completed flag. PATCH (partial update) rather than PUT, so the
     * frontend can mark a reminder done without resending the whole object.
     */
    @Transactional
    public ReminderResponse toggleCompleted(Long userId, Long reminderId, Boolean completed) {
        Reminder reminder = loadOwnedReminder(userId, reminderId);
        reminder.setCompleted(completed == null ? !reminder.isCompleted() : completed);
        return ReminderResponse.from(reminderRepository.save(reminder));
    }

    @Transactional
    public void delete(Long userId, Long reminderId) {
        Reminder reminder = loadOwnedReminder(userId, reminderId);
        reminderRepository.delete(reminder);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private Reminder loadOwnedReminder(Long userId, Long reminderId) {
        return reminderRepository.findByIdAndUserId(reminderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reminder " + reminderId + " does not exist for this account."));
    }

    private void applyRequest(Reminder reminder, ReminderRequest request) {
        reminder.setTitle(request.getTitle().trim());
        reminder.setNotes(blankToNull(request.getNotes()));
        reminder.setDate(request.getDate());
        reminder.setTime(request.getTime());
        reminder.setPriority(request.getPriority() == null ? Priority.MEDIUM : request.getPriority());
        reminder.setCategory(blankToNull(request.getCategory()));
    }

    private void requireFutureDateTime(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            throw new InvalidReminderTimeException("Both a date and a time are required.");
        }
        LocalDateTime dueAt = LocalDateTime.of(date, time);
        if (dueAt.isBefore(LocalDateTime.now())) {
            throw new InvalidReminderTimeException(
                    "That time is in the past. Pick a date and time in the future so the alarm can ring.");
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
