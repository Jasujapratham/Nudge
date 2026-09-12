package com.nudge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.nudge.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Payload for creating (POST) or editing (PUT) a reminder.
 *
 * <p>The frontend sends {@code "date": "2026-09-20"} and {@code "time": "18:30"},
 * which Jackson maps to LocalDate / LocalTime thanks to the @JsonFormat patterns.</p>
 */
public class ReminderRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 120, message = "Title must be at most 120 characters")
    private String title;

    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;

    @NotNull(message = "Date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "Time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime time;

    @NotNull(message = "Priority is required")
    private Priority priority = Priority.MEDIUM;

    @Size(max = 60, message = "Category must be at most 60 characters")
    private String category;

    /** Only used when editing: lets the client move a reminder back to pending. */
    private Boolean completed;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
