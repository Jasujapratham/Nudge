package com.nudge.dto;

public record AIReminderResponse(
        String title,
        String notes,
        String date,
        String time,
        String priority,
        String category,
        boolean needsClarification,
        String clarificationQuestion
) {}
