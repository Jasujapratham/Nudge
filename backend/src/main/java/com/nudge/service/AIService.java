package com.nudge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nudge.dto.AIReminderRequest;
import com.nudge.dto.AIReminderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AIService {
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AIService(ObjectMapper objectMapper,
                     @Value("${nudge.gemini.api-key:}") String apiKey,
                     @Value("${nudge.gemini.model:gemini-2.5-flash}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public AIReminderResponse parseReminder(AIReminderRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini is not configured. Set GEMINI_API_KEY before starting Nudge.");
        }

        String systemPrompt = """
                You are Nudge, a reminder parsing assistant. Convert the user's natural-language request into JSON.
                Return ONLY a JSON object with exactly these keys:
                title, notes, date, time, priority, category, needsClarification, clarificationQuestion.
                date must be YYYY-MM-DD and time must be HH:mm in the user's local timezone.
                Use the supplied current date/time to resolve words such as today, tomorrow, next Monday, and in 2 hours.
                priority must be exactly LOW, MEDIUM, or HIGH. Default to MEDIUM.
                category can be Work, Study, Personal, Health, Bills, Errands, or null.
                If date or time is genuinely missing or ambiguous, set needsClarification=true and provide a concise clarificationQuestion.
                Never invent a date/time when the request is ambiguous. If the user gives no notes, use null.
                """;

        String userPrompt = "User request: " + request.text()
                + "\nCurrent local date: " + safe(request.currentDate())
                + "\nCurrent local time: " + safe(request.currentTime())
                + "\nTimezone: " + safe(request.timezone());

        String prompt = systemPrompt + "\n\n" + userPrompt;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", new Object[]{
                Map.of("parts", new Object[]{Map.of("text", prompt)})
        });
        body.put("generationConfig", Map.of(
                "temperature", 0.1,
                "responseMimeType", "application/json"
        ));

        try {
            String raw = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            String content = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("Gemini returned an empty response.");
            }
            return objectMapper.readValue(content, AIReminderResponse.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini reminder parsing failed: " + exception.getMessage(), exception);
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "not provided" : value;
    }
}
