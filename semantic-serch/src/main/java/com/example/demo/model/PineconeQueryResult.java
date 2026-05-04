package com.example.demo.model;

import java.util.List;
import java.util.Map;

/**
 * Enhanced DTO for Pinecone query results.
 * Supports advanced ranking + debugging.
 */
public class PineconeQueryResult {

    private String id;
    private Float score;
    private Map<String, Object> metadata;

    // 🔥 NEW FIELDS
    private List<Float> values;     // optional: embedding vector
    private String namespace;

    public PineconeQueryResult() {}

    public PineconeQueryResult(String id, Float score, Map<String, Object> metadata) {
        this.id = id;
        this.score = score;
        this.metadata = metadata;
    }

    // =====================
    // GETTERS / SETTERS
    // =====================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<Float> getValues() {
        return values;
    }

    public void setValues(List<Float> values) {
        this.values = values;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    // =====================
    // 🔥 HELPER METHODS
    // =====================

    public String getText() {
        if (metadata == null) return "";
        return String.valueOf(metadata.getOrDefault("text", ""));
    }

    public String getFilename() {
        if (metadata == null) return null;
        Object f = metadata.get("filename");
        return f != null ? f.toString() : null;
    }

    public Integer getChunkIndex() {
        if (metadata == null) return null;
        Object c = metadata.get("chunkIndex");
        if (c instanceof Number) return ((Number) c).intValue();
        return null;
    }

    public boolean hasText() {
        return metadata != null && metadata.get("text") != null;
    }

    @Override
    public String toString() {
        return "PineconeQueryResult{" +
                "id='" + id + '\'' +
                ", score=" + score +
                ", textPreview='" + getText().substring(0, Math.min(50, getText().length())) + "...'" +
                '}';
    }
}