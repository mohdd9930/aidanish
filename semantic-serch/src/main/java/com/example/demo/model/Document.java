package com.example.demo.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a document or chunk stored in the system.
 * Optimized for RAG + debugging + traceability.
 */
public class Document {

    private String id;
    private String text;

    // 🔥 NEW: metadata (very important)
    private Map<String, Object> metadata;

    // 🔥 NEW: timestamps (production grade)
    private Instant createdAt;

    public Document() {
    }

    public Document(String id, String text, Map<String, Object> metadata) {
        this.id = id;
        this.text = text;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }

    // =========================
    // GETTERS & SETTERS
    // =========================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // =========================
    // DEBUG FRIENDLY
    // =========================

    @Override
    public String toString() {
        return "Document{" +
                "id='" + id + '\'' +
                ", textLength=" + (text != null ? text.length() : 0) +
                ", metadata=" + metadata +
                ", createdAt=" + createdAt +
                '}';
    }
}