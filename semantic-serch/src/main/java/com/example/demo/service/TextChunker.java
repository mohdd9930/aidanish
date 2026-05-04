package com.example.demo.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP_SIZE = 80;

    public List<String> chunk(String text) {

        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String normalized = text.replaceAll("\\s+", " ").trim();

        String[] sentences = normalized.split("(?<=[.!?])\\s+");

        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {

            if (current.length() + sentence.length() > CHUNK_SIZE) {

                if (!current.isEmpty()) {
                    String chunk = current.toString().trim();
                    chunks.add(chunk);

                    // SAFE overlap from last chunk
                    current = new StringBuilder(getOverlap(chunk));
                    current.append(" ");
                } else {
                    current = new StringBuilder();
                }
            }

            current.append(sentence).append(" ");
        }

        if (!current.isEmpty()) {
            chunks.add(current.toString().trim());
        }

        return chunks;
    }

    // safer overlap (word-safe)
    private String getOverlap(String text) {

        if (text.length() <= OVERLAP_SIZE) return text;

        String cut = text.substring(text.length() - OVERLAP_SIZE);

        int spaceIndex = cut.indexOf(" ");
        return (spaceIndex > 0) ? cut.substring(spaceIndex + 1) : cut;
    }
}