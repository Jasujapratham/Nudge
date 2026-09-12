package com.nudge.controller;

import com.nudge.dto.CompleteRequest;
import com.nudge.dto.ReminderRequest;
import com.nudge.dto.ReminderResponse;
import com.nudge.security.AuthenticatedUser;
import com.nudge.service.ReminderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reminder endpoints. All of them are protected by JWT and scoped to the
 * logged-in user, which Spring Security resolves into the
 * {@link AuthenticatedUser} principal.
 */
@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /** GET /api/reminders - this user's reminders, oldest first. */
    @GetMapping
    public ResponseEntity<List<ReminderResponse>> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(reminderService.findAllForUser(user.id()));
    }

    /** POST /api/reminders - create one. Returns 201 and the created object. */
    @PostMapping
    public ResponseEntity<ReminderResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @Valid @RequestBody ReminderRequest request) {
        ReminderResponse created = reminderService.create(user.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** GET /api/reminders/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ReminderResponse> getOne(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @PathVariable Long id) {
        return ResponseEntity.ok(reminderService.findByIdForUser(user.id(), id));
    }

    /** PUT /api/reminders/{id} - full edit. */
    @PutMapping("/{id}")
    public ResponseEntity<ReminderResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
                                                   @PathVariable Long id,
                                                   @Valid @RequestBody ReminderRequest request) {
        return ResponseEntity.ok(reminderService.update(user.id(), id, request));
    }

    /**
     * PATCH /api/reminders/{id}/complete - mark completed or back to pending.
     * The body may be omitted entirely, in which case the flag is toggled.
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<ReminderResponse> toggleComplete(@AuthenticationPrincipal AuthenticatedUser user,
                                                           @PathVariable Long id,
                                                           @RequestBody(required = false) CompleteRequest request) {
        Boolean completed = request == null ? null : request.completed();
        return ResponseEntity.ok(reminderService.toggleCompleted(user.id(), id, completed));
    }

    /** DELETE /api/reminders/{id} - returns 204 with no body. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                       @PathVariable Long id) {
        reminderService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
