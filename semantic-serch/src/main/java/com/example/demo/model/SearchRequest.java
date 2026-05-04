package com.example.demo.model;

/**
 * Advanced Search Request DTO.
 * Supports configurable retrieval + better RAG control.
 */
public class SearchRequest {

    private String text;

    // 🔥 NEW FIELDS
    private Integer topK = 5;              // how many chunks to retrieve
    private Double minScore = 0.7;         // similarity threshold
    private Boolean useRerank = true;      // enable keyword reranking
    private Boolean useLLM = true;         // use Gemini or not
    private Integer maxTokens = 150;       // answer size control

    public SearchRequest() {}

    public SearchRequest(String text) {
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

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public Double getMinScore() {
        return minScore;
    }

    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }

    public Boolean getUseRerank() {
        return useRerank;
    }

    public void setUseRerank(Boolean useRerank) {
        this.useRerank = useRerank;
    }

    public Boolean getUseLLM() {
        return useLLM;
    }

    public void setUseLLM(Boolean useLLM) {
        this.useLLM = useLLM;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    // =====================
    // DEBUG
    // =====================

    @Override
    public String toString() {
        return "SearchRequest{" +
                "text='" + text + '\'' +
                ", topK=" + topK +
                ", minScore=" + minScore +
                ", useRerank=" + useRerank +
                ", useLLM=" + useLLM +
                ", maxTokens=" + maxTokens +
                '}';
    }
}