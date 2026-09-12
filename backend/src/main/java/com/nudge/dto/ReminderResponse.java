package com.nudge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nudge.entity.Priority;
import com.nudge.entity.Reminder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Reminder as the frontend sees it. Includes a convenience {@code dueAt}
 * timestamp that the browser alarm uses to decide when to ring.
 */
public class ReminderResponse {

    private final Long id;
    private final String title;
    private final String notes;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate date;

    @JsonFormat(pattern = "HH:mm")
    private final LocalTime time;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private final LocalDateTime dueAt;

    private final Priority priority;
    private final String category;
    private final boolean completed;
    private final LocalDateTime createdAt;

    public ReminderResponse(Long id, String title, String notes, LocalDate date, LocalTime time,
                            LocalDateTime dueAt, Priority priority, String category,
                            boolean completed, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.notes = notes;
        this.date = date;
        this.time = time;
        this.dueAt = dueAt;
        this.priority = priority;
        this.category = category;
        this.completed = completed;
        this.createdAt = createdAt;
    }

    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getNotes(),
                reminder.getDate(),
                reminder.getTime(),
                reminder.getDueAt(),
                reminder.getPriority(),
                reminder.getCategory(),
                reminder.isCompleted(),
                reminder.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public Priority getPriority() {
        return priority;
    }

    public String getCategory() {
        return category;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
