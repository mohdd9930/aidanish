package com.example.demo.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileTextExtractor {

    private static final Logger logger = LoggerFactory.getLogger(FileTextExtractor.class);
    private static final int MAX_CHARS = 1_000_000;

    private final Tika tika = new Tika();

    public FileTextExtractor() {
        tika.setMaxStringLength(MAX_CHARS);
    }

    public String extract(InputStream in, String filename) {
        try {
            String rawText = tika.parseToString(in);
            if (rawText == null) rawText = "";

            String cleaned = cleanText(rawText);

            logger.info("Extracted {} chars (cleaned {}) from '{}'",
                    rawText.length(), cleaned.length(), filename);

            return cleaned;

        } catch (IOException | TikaException e) {
            logger.error("Failed to extract text from '{}'", filename, e);
            throw new ExtractionException(
                    "Could not read file '" + filename + "'. Supported: PDF, DOC, DOCX, TXT, RTF, ODT, HTML."
            );
        }
    }

    // 🔥 CLEANING LOGIC (THIS IMPROVES RAG ACCURACY)
    private String cleanText(String text) {

        return text
                // remove multiple spaces/newlines
                .replaceAll("\\s+", " ")

                // remove page numbers like "Page 1", "Page 12"
                .replaceAll("(?i)page \\d+", "")

                // remove standalone numbers (common in PDFs)
                .replaceAll("\\n?\\s*\\d+\\s*\\n", " ")

                // remove non-ASCII (weird PDF chars)
                .replaceAll("[^\\x00-\\x7F]", "")

                // remove repeated punctuation
                .replaceAll("[\\.]{2,}", ".")

                // trim
                .trim();
    }

    public static class ExtractionException extends RuntimeException {
        public ExtractionException(String message) {
            super(message);
        }
    }
}