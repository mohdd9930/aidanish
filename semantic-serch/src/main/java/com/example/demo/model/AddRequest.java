package com.example.demo.model;

/**
 * Advanced request DTO for adding documents.
 * Supports metadata + better indexing control.
 */
public class AddRequest {

    private String text;

    // 🔥 NEW FIELDS
    private String filename;        // source name
    private String documentId;      // optional grouping ID
    private Boolean chunk = true;   // whether to chunk or not
    private Integer maxChunks = 500;

    public AddRequest() {}

    public AddRequest(String text) {
        this.text = text;
    }

    // =====================
    // GETTERS / SETTERS
    // =====================

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public Boolean getChunk() {
        return chunk;
    }

    public void setChunk(Boolean chunk) {
        this.chunk = chunk;
    }

    public Integer getMaxChunks() {
        return maxChunks;
    }

    public void setMaxChunks(Integer maxChunks) {
        this.maxChunks = maxChunks;
    }

    @Override
    public String toString() {
        return "AddRequest{" +
                "text='" + text + '\'' +
                ", filename='" + filename + '\'' +
                ", documentId='" + documentId + '\'' +
                ", chunk=" + chunk +
                ", maxChunks=" + maxChunks +
                '}';
    }
}