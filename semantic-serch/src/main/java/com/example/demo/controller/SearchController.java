package com.example.demo.controller;

import com.example.demo.model.AddRequest;
import com.example.demo.model.Document;
import com.example.demo.model.PineconeQueryResult;
import com.example.demo.model.SearchRequest;
import com.example.demo.repository.PineconeRepository;
import com.example.demo.service.FileTextExtractor;
import com.example.demo.service.GeminiClient;
import com.example.demo.service.SemanticSearchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api")
public class SearchController {

    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    private SemanticSearchService semanticSearchService;

    @Autowired
    private GeminiClient geminiClient;

    @Autowired
    private FileTextExtractor fileTextExtractor;

    // =========================
    // 📥 FILE UPLOAD
    // =========================

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return badRequest("No file uploaded");
            }

            String filename = sanitizeFilename(file.getOriginalFilename());
            logger.info("Uploading: {}", filename);

            String text;
            try (var in = file.getInputStream()) {
                text = fileTextExtractor.extract(in, filename);
            }

            if (text.trim().isEmpty()) {
                return badRequest("File has no readable content");
            }

            int chunks = semanticSearchService.addFileChunks(filename, text);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "filename", filename,
                    "chunks", chunks
            ));

        } catch (Exception e) {
            logger.error("Upload error", e);
            return serverError("Upload failed");
        }
    }

    // =========================
    // ➕ ADD TEXT
    // =========================

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody AddRequest request) {
        try {
            if (request.getText() == null || request.getText().isBlank()) {
                return badRequest("Text required");
            }

            Document doc = semanticSearchService.addDocument(request.getText());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "id", doc.getId()
            ));

        } catch (Exception e) {
            logger.error("Add error", e);
            return serverError("Failed to add document");
        }
    }

    // =========================
    // 🔥 MAIN RAG ENDPOINT (UPGRADED)
    // =========================

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody SearchRequest request) {

        try {
            if (request.getText() == null || request.getText().isBlank()) {
                return badRequest("Question required");
            }

            String question = request.getText().trim();
            logger.info("QUESTION: {}", question);

            // ✅ USE YOUR NEW SMART RETRIEVAL
            List<PineconeQueryResult> chunks =
                    semanticSearchService.searchRelevantChunks(question);

            // ❌ No context
            if (chunks.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "answer", "I don't know.",
                        "sources", List.of()
                ));
            }

            // ✅ Build clean context
            String context = semanticSearchService.buildContext(chunks);

            // 🔥 BEST PROMPT (HIGH ACCURACY)
        
                    String prompt =
                                        "You are an intelligent assistant.\n" +
                                        "Answer the question using the context.\n\n" +

                                        "Rules:\n" +
                                        "- Understand the meaning of the question\n" +
                                        "- Answer in your own words\n" +
                                        "- Keep answer short (2-3 sentences)\n" +
                                        "- If partial info exists, still answer\n" +
                                        "- Only say 'I don't know' if nothing is relevant\n\n" +

                                        "Context:\n" + context + "\n\n" +
                                        "Question:\n" + question + "\n\n" +
                                        "Answer:";

            String answer = geminiClient.generate(prompt);

            // ✅ Build sources (clean)
            List<Map<String, Object>> sources = new ArrayList<>();
            for (PineconeQueryResult r : chunks) {
                sources.add(Map.of(
                        "text", truncate(getText(r), 300),
                        "score", r.getScore()
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "answer", answer,
                    "sources", sources
            ));

        } catch (Exception e) {
            logger.error("Ask error", e);
            return serverError("Failed to answer");
        }
    }

    // =========================
    // ❤️ HEALTH
    // =========================

    @PostMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    // =========================
    // 🧠 HELPERS
    // =========================

    private String sanitizeFilename(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getText(PineconeQueryResult r) {
        return (String) r.getMetadata().getOrDefault("text", "");
    }

    private String truncate(String text, int max) {
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private ResponseEntity<?> badRequest(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", msg));
    }

    private ResponseEntity<?> serverError(String msg) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", msg));
    }
}