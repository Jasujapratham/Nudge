package com.nudge.repository;

import com.nudge.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access layer for reminders. Every query is scoped to the owning user,
 * which is what keeps one user's data invisible to another user.
 */
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByUserIdOrderByDateAscTimeAsc(Long userId);

    Optional<Reminder> findByIdAndUserId(Long id, Long userId);
}
