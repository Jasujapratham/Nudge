package com.nudge.dto;

/**
 * Optional body for PATCH /api/reminders/{id}/complete.
 *
 * <p>{@code completed=true} marks the reminder done, {@code false} puts it back
 * to pending, and omitting the body entirely toggles the current state.</p>
 */
public record CompleteRequest(Boolean completed) {
}
