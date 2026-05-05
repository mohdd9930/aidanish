package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BM25Service {

    private static final Logger logger =
            LoggerFactory.getLogger(BM25Service.class);

    // word -> (docId -> frequency)
    private final Map<String, Map<String, Integer>> index = new HashMap<>();

    // =========================
    // INDEX
    // =========================
    public void index(String docId, String text) {

        if (text == null || text.isBlank()) {
            logger.warn("⚠️ Empty text received for docId={}", docId);
            return;
        }

        logger.debug("📥 Indexing docId={}", docId);

        String[] words = text.toLowerCase().split("\\s+");

        for (String word : words) {

            if (word.length() < 2) continue;

            index.putIfAbsent(word, new HashMap<>());

            Map<String, Integer> postings = index.get(word);

            postings.put(docId,
                    postings.getOrDefault(docId, 0) + 1);
        }

        logger.debug("✅ Indexed docId={} with {} words",
                docId, words.length);
    }

    // =========================
    // SEARCH
    // =========================
    public Map<String, Double> search(String query) {

        logger.info("🔍 BM25 search query: {}", query);

        Map<String, Double> scores = new HashMap<>();

        if (query == null || query.isBlank()) {
            logger.warn("⚠️ Empty query received");
            return scores;
        }

        String[] words = query.toLowerCase().split("\\s+");

        for (String word : words) {

            Map<String, Integer> postings = index.get(word);

            if (postings == null) {
                logger.debug("❌ No postings for word={}", word);
                continue;
            }

            for (var entry : postings.entrySet()) {

                scores.put(
                        entry.getKey(),
                        scores.getOrDefault(entry.getKey(), 0.0)
                                + entry.getValue()
                );
            }
        }

        logger.info("📊 BM25 results found: {}", scores.size());

        return scores;
    }

    // =========================
    // DEBUG HELPER
    // =========================
    public void printIndexStats() {

        logger.info("📚 BM25 Index Size: {} terms", index.size());
    }
}