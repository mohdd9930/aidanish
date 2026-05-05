package com.example.demo.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import com.example.demo.model.PineconeQueryResult;
import com.example.demo.model.PineconeVector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class PineconeRepository {

    private static final Logger logger = LoggerFactory.getLogger(PineconeRepository.class);

    @Autowired
    private RestTemplate restTemplate;

    @Value("${pinecone.api-key}")
    private String apiKey;

    @Value("${pinecone.index-url}")
    private String indexUrl;

    // 🔥 NEW: namespace support
    @Value("${pinecone.namespace:default}")
    private String namespace;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_RETRIES = 2;

    // =========================
    // 📥 UPSERT
    // =========================

    public boolean upsertVector(PineconeVector vector) {
        try {
            String url = indexUrl + "/vectors/upsert";

            Map<String, Object> body = Map.of(
                    "vectors", List.of(vector),
                    "namespace", namespace
            );

            ResponseEntity<String> response = callWithRetry(url, body);

            if (response.getStatusCode().is2xxSuccessful()) {
                return true;
            }

            logger.error("Upsert failed: {}", response.getStatusCode());
            return false;

        } catch (Exception e) {
            logger.error("Upsert error", e);
            return false;
        }
    }

    // =========================
    // 🔍 QUERY TOP K (IMPROVED)
    // =========================

    public List<PineconeQueryResult> queryTopK(List<Float> vector, int topK) {

        try {
            String url = indexUrl + "/query";

            Map<String, Object> body = new HashMap<>();
            body.put("vector", vector);
            body.put("topK", topK);
            body.put("includeMetadata", true);
            body.put("namespace", namespace);

            // 🔥 Optional filter support (extend later)
            // body.put("filter", Map.of("filename", "doc.pdf"));

            ResponseEntity<String> response = callWithRetry(url, body);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new PineconeException("Query failed: " + response.getStatusCode());
            }

            List<PineconeQueryResult> results = parseMatches(response.getBody());

            // 🔥 IMPORTANT: normalize scores
            normalizeScores(results);

            return results;

        } catch (Exception e) {
            logger.error("Query error", e);
            throw new PineconeException("Vector search failed");
        }
    }

    // =========================
    // 🔁 RETRY LOGIC
    // =========================

    private ResponseEntity<String> callWithRetry(String url, Map<String, Object> body) {

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, createHeaders());

        int attempt = 0;

        while (attempt <= MAX_RETRIES) {
            try {
                return restTemplate.postForEntity(url, entity, String.class);
            } catch (Exception e) {
                attempt++;
                logger.warn("Retry {} for Pinecone...", attempt);

                if (attempt > MAX_RETRIES) {
                    throw e;
                }
            }
        }

        throw new RuntimeException("Unreachable");
    }

    // =========================
    // 📊 PARSE RESPONSE
    // =========================

    private List<PineconeQueryResult> parseMatches(String body) {

        List<PineconeQueryResult> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode matches = root.get("matches");

            if (matches == null || !matches.isArray()) return results;

            for (JsonNode m : matches) {

                String id = m.get("id").asText();
                float score = m.get("score").floatValue();

                Map<String, Object> metadata =
                        objectMapper.convertValue(
                                m.get("metadata"),
                                new com.fasterxml.jackson.core.type.TypeReference<>() {}
                        );

                results.add(new PineconeQueryResult(id, score, metadata));
            }

        } catch (Exception e) {
            logger.error("Parse error", e);
        }

        return results;
    }

    // =========================
    // 📈 SCORE NORMALIZATION
    // =========================

    private void normalizeScores(List<PineconeQueryResult> results) {

        if (results.isEmpty()) return;

        float max = results.stream()
                .map(PineconeQueryResult::getScore)
                .max(Float::compare)
                .orElse(1f);

        for (PineconeQueryResult r : results) {
            float normalized = r.getScore() / max;
            r.setScore(normalized);
        }
    }

    // =========================
    // 🧠 HEADERS
    // =========================

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Api-Key", apiKey);
        return headers;
    }

    // =========================
    // ❌ ERROR CLASS
    // =========================

    public static class PineconeException extends RuntimeException {
        public PineconeException(String msg) {
            super(msg);
        }
    }
}