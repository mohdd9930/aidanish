package com.example.demo.model;

import java.util.List;
import java.util.Map;

/**
 * Advanced response DTO for search + RAG answers.
 * Supports answer, sources, and debug info.
 */
public class SearchResponse {

    // 🔥 FINAL ANSWER (LLM output)
    private String answer;

    // 🔍 RAW MATCH (fallback / non-LLM)
    private String text;

    private Float score;

    // 📚 SOURCES (VERY IMPORTANT for RAG)
    private List<Map<String, Object>> sources;

    // ⚙️ DEBUG INFO
    private Integer totalMatches;
    private Boolean usedLLM;

    public SearchResponse() {}

    // =====================
    // CONSTRUCTORS
    // =====================

    public SearchResponse(String text, Float score) {
        this.text = text;
        this.score = score;
    }

    public SearchResponse(String answer, List<Map<String, Object>> sources) {
        this.answer = answer;
        this.sources = sources;
        this.usedLLM = true;
    }

    // =====================
    // GETTERS / SETTERS
    // =====================

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public List<Map<String, Object>> getSources() {
        return sources;
    }

    public void setSources(List<Map<String, Object>> sources) {
        this.sources = sources;
    }

    public Integer getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(Integer totalMatches) {
        this.totalMatches = totalMatches;
    }

    public Boolean getUsedLLM() {
        return usedLLM;
    }

    public void setUsedLLM(Boolean usedLLM) {
        this.usedLLM = usedLLM;
    }

    // =====================
    // DEBUG
    // =====================

    @Override
    public String toString() {
        return "SearchResponse{" +
                "answer='" + answer + '\'' +
                ", text='" + text + '\'' +
                ", score=" + score +
                ", sourcesCount=" + (sources != null ? sources.size() : 0) +
                ", usedLLM=" + usedLLM +
                '}';
    }
}