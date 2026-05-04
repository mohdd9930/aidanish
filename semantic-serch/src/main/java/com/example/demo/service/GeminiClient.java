package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Minimal client for Google Gemini's Generative Language REST API.
 * Calls https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
 */
@Component
public class GeminiClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.0-flash}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sends a prompt to Gemini and returns the generated text.
     */
    public String generate(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        String url = String.format(ENDPOINT, model);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Gemini returned non-2xx status: {}", response.getStatusCode());
                throw new GeminiException("Gemini returned status " + response.getStatusCode().value());
            }
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode candidates = root.get("candidates");
            if (candidates == null || !candidates.isArray() || candidates.size() == 0) {
                logger.warn("Gemini response had no candidates");
                throw new GeminiException("Gemini returned no candidates");
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.size() == 0) {
                throw new GeminiException("Gemini candidate had no parts");
            }
            return parts.get(0).path("text").asText("").trim();
        } catch (GeminiException e) {
            throw e;
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            logger.warn("Gemini quota exceeded (429) for model={}", model);
            throw new GeminiException("Gemini free-tier quota reached for model '" + model + "'. The daily limit (often only ~20–50 requests) may be exhausted; it resets at midnight Pacific time. Try a different model in application.properties (e.g. gemini-2.5-flash, gemini-2.5-flash-lite) or wait.");
        } catch (Exception e) {
            logger.error("Error calling Gemini (model={})", model, e);
            throw new GeminiException("Failed to call Gemini");
        }
    }

    public static class GeminiException extends RuntimeException {
        public GeminiException(String message) { super(message); }
    }
}
