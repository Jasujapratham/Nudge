package com.nudge.dto;

import jakarta.validation.constraints.NotBlank;

public record AIReminderRequest(
        @NotBlank(message = "Describe the reminder you want to create.") String text,
        String currentDate,
        String currentTime,
        String timezone
) {}
