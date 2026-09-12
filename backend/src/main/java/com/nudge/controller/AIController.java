package com.nudge.controller;

import com.nudge.dto.AIReminderRequest;
import com.nudge.dto.AIReminderResponse;
import com.nudge.service.AIService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.nudge.security.AuthenticatedUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/parse-reminder")
    public ResponseEntity<AIReminderResponse> parseReminder(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AIReminderRequest request) {
        return ResponseEntity.ok(aiService.parseReminder(request));
    }
}
