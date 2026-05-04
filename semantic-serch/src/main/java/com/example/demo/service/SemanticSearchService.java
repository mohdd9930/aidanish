package com.example.demo.service;

import java.util.*;
import java.util.stream.Collectors;

import com.example.demo.model.Document;
import com.example.demo.model.PineconeQueryResult;
import com.example.demo.model.PineconeVector;
import com.example.demo.repository.PineconeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SemanticSearchService {

    private static final Logger logger =
            LoggerFactory.getLogger(SemanticSearchService.class);

    @Autowired
    private PineconeRepository pineconeRepository;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private TextChunker textChunker;

    @Autowired
    private BM25Service bm25Service;

    private static final int TOP_K = 8;
    private static final int FINAL_CONTEXT_CHUNKS = 5;
    private static final double SIMILARITY_THRESHOLD = 0.55;

    // =========================
    // INDEXING
    // =========================
    public int addFileChunks(String filename, String fullText) {

        logger.info("📥 Starting indexing for file: {}", filename);

        List<String> chunks = textChunker.chunk(fullText);

        logger.info("✂️ Total chunks created: {}", chunks.size());

        if (chunks.isEmpty()) {
            logger.error("❌ No text found in file: {}", filename);
            throw new RuntimeException("No text found");
        }

        int count = 0;

        for (int i = 0; i < chunks.size(); i++) {

            String chunkText = chunks.get(i);

            logger.debug("Processing chunk {}: {}", i, chunkText.substring(0, Math.min(50, chunkText.length())));

            List<Float> embedding = generateEmbedding(chunkText);

            String id = UUID.randomUUID().toString();

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("text", chunkText);
            metadata.put("filename", filename);
            metadata.put("chunkIndex", i);
            metadata.put("docId", id);

            PineconeVector vector =
                    new PineconeVector(id, embedding, metadata);

            boolean stored = pineconeRepository.upsertVector(vector);

            if (stored) {
                count++;
                logger.debug("✅ Stored chunk {} in Pinecone", i);
            } else {
                logger.warn("⚠️ Failed to store chunk {}", i);
            }

            bm25Service.index(id, chunkText);
        }

        logger.info("✅ Indexing completed. Total stored chunks: {}", count);

        return count;
    }

    // =========================
    // HYBRID SEARCH
    // =========================
    public List<PineconeQueryResult> searchRelevantChunks(String queryText) {

        logger.info("🔍 Search query received: {}", queryText);

        String cleanQuery = cleanQuery(queryText);

        logger.debug("🧹 Clean query: {}", cleanQuery);

        List<PineconeQueryResult> vectorResults = new ArrayList<>();

        for (String q : expandQuery(cleanQuery)) {

            logger.debug("➡️ Expanded query: {}", q);

            List<Float> emb = generateEmbedding(q);

            List<PineconeQueryResult> res =
                    pineconeRepository.queryTopK(emb, TOP_K);

            logger.debug("📊 Vector results for '{}': {}", q,
                    res != null ? res.size() : 0);

            if (res != null) vectorResults.addAll(res);
        }

        Map<String, Double> bm25Scores =
                bm25Service.search(cleanQuery);

        logger.debug("📚 BM25 results found: {}", bm25Scores.size());

        if (vectorResults.isEmpty()) {
            logger.warn("❌ No vector results found");
            return Collections.emptyList();
        }

        List<PineconeQueryResult> finalResults =
                vectorResults.stream()
                        .filter(r -> r.getScore() >= SIMILARITY_THRESHOLD)
                        .sorted((a, b) -> {

                            String textA = getText(a);
                            String textB = getText(b);

                            double scoreA =
                                    a.getScore()
                                            + 0.3 * normalize(bm25Scores.getOrDefault(textA, 0.0));

                            double scoreB =
                                    b.getScore()
                                            + 0.3 * normalize(bm25Scores.getOrDefault(textB, 0.0));

                            return Double.compare(scoreB, scoreA);
                        })
                        .limit(FINAL_CONTEXT_CHUNKS)
                        .collect(Collectors.toList());

        logger.info("🎯 Final results returned: {}", finalResults.size());

        return finalResults;
    }

    // =========================
    // CONTEXT BUILDER
    // =========================
    public String buildContext(List<PineconeQueryResult> chunks) {

        logger.info("🧱 Building context from {} chunks", chunks.size());

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < chunks.size(); i++) {

            String text = getText(chunks.get(i));

            sb.append("[").append(i + 1).append("] ")
                    .append(text)
                    .append("\n");

            logger.debug("Added chunk {} to context", i);
        }

        return sb.toString();
    }

    // =========================
    // ANSWER
    // =========================
    public String generateAnswer(String queryText) {

        logger.info("💡 Generating answer...");

        List<PineconeQueryResult> results =
                searchRelevantChunks(queryText);

        if (results.isEmpty()) {
            logger.warn("⚠️ No relevant results found");
            return "No relevant information found.";
        }

        String context = buildContext(results);

        logger.info("✅ Answer generated successfully");

        return "Answer:\n\n" + context;
    }

    // =========================
    // EMBEDDING
    // =========================
    private List<Float> generateEmbedding(String text) {

        logger.debug("🧠 Generating embedding");

        var emb = embeddingModel.embed(text);

        List<Float> result = new ArrayList<>();

        for (double v : emb) {
            result.add((float) v);
        }

        return result;
    }

    // =========================
    // HELPERS
    // =========================
    private List<String> expandQuery(String query) {

        return List.of(
                query,
                "what is " + query,
                "explain " + query,
                query + " details"
        );
    }

    private String cleanQuery(String query) {

        return query.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\b(the|is|and|or|of|to|in|on|for|with)\\b", "")
                .trim();
    }

    private String getText(PineconeQueryResult r) {
        return (String) r.getMetadata().getOrDefault("text", "");
    }

    private double normalize(double score) {
        return Math.min(score / 10.0, 1.0);
    }

    // =========================
    // ADD DOCUMENT
    // =========================
    public Document addDocument(String text) {

        logger.info("📄 Adding document manually");

        addFileChunks("manual_input", text);

        logger.info("✅ Document added successfully");

        return new Document(
                UUID.randomUUID().toString(),
                text,
                null
        );
    }
}