package com.example.demo.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced DTO for Pinecone vector storage.
 * Supports structured metadata for better retrieval accuracy.
 */
public class PineconeVector {

    private String id;
    private List<Float> values;
    private Map<String, Object> metadata = new HashMap<>();

    public PineconeVector() {}

    public PineconeVector(String id, List<Float> values, Map<String, Object> metadata) {
        this.id = id;
        this.values = values;
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
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

    public List<Float> getValues() {
        return values;
    }

    public void setValues(List<Float> values) {
        this.values = values;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // =====================
    // 🔥 HELPER METHODS
    // =====================

    public void addText(String text) {
        metadata.put("text", text);
    }

    public void addFilename(String filename) {
        metadata.put("filename", filename);
    }

    public void addChunkIndex(int index) {
        metadata.put("chunkIndex", index);
    }

    public void addSection(String section) {
        metadata.put("section", section); // optional: heading/title
    }

    public void addPageNumber(int page) {
        metadata.put("page", page); // useful for PDFs
    }

    public void addCustom(String key, Object value) {
        metadata.put(key, value);
    }

    // =====================
    // 🔥 FACTORY METHOD (VERY IMPORTANT)
    // =====================

    public static PineconeVector fromChunk(
            String id,
            List<Float> embedding,
            String text,
            String filename,
            int chunkIndex
    ) {
        PineconeVector vector = new PineconeVector();
        vector.setId(id);
        vector.setValues(embedding);

        vector.addText(text);
        vector.addFilename(filename);
        vector.addChunkIndex(chunkIndex);

        return vector;
    }

    @Override
    public String toString() {
        return "PineconeVector{" +
                "id='" + id + '\'' +
                ", valuesSize=" + (values != null ? values.size() : 0) +
                ", metadataKeys=" + metadata.keySet() +
                '}';
    }
}