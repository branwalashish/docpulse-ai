package com.docpulse.ai.ingestion.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing the result of parsing a document via Apache Tika.
 * Contains extracted text and document properties prior to chunking and embedding.
 */
public class ParsedDocument {

    private final String documentId;
    private final String documentName;
    private final String rawText;
    private final String detectedMimeType;
    private final int characterCount;
    private final int wordCount;
    private final Map<String, Object> documentProperties;

    private ParsedDocument(Builder builder) {
        this.documentId = builder.documentId != null ? builder.documentId : UUID.randomUUID().toString();
        this.documentName = Objects.requireNonNull(builder.documentName, "documentName cannot be null");
        this.rawText = builder.rawText != null ? builder.rawText : "";
        this.detectedMimeType = builder.detectedMimeType != null ? builder.detectedMimeType : "application/octet-stream";
        this.characterCount = this.rawText.length();
        this.wordCount = calculateWordCount(this.rawText);
        this.documentProperties = builder.documentProperties != null
                ? Map.copyOf(builder.documentProperties)
                : Collections.emptyMap();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean hasContent() {
        return !rawText.isBlank();
    }

    private static int calculateWordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String[] words = text.trim().split("\\s+");
        return words.length;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getRawText() {
        return rawText;
    }

    public String getDetectedMimeType() {
        return detectedMimeType;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public int getWordCount() {
        return wordCount;
    }

    public Map<String, Object> getDocumentProperties() {
        return documentProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParsedDocument that)) return false;
        return Objects.equals(documentId, that.documentId) &&
                Objects.equals(documentName, that.documentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId, documentName);
    }

    @Override
    public String toString() {
        return "ParsedDocument{" +
                "documentId='" + documentId + '\'' +
                ", documentName='" + documentName + '\'' +
                ", detectedMimeType='" + detectedMimeType + '\'' +
                ", characterCount=" + characterCount +
                ", wordCount=" + wordCount +
                '}';
    }

    public static class Builder {
        private String documentId;
        private String documentName;
        private String rawText;
        private String detectedMimeType;
        private Map<String, Object> documentProperties = new HashMap<>();

        public Builder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder documentName(String documentName) {
            this.documentName = documentName;
            return this;
        }

        public Builder rawText(String rawText) {
            this.rawText = rawText;
            return this;
        }

        public Builder detectedMimeType(String detectedMimeType) {
            this.detectedMimeType = detectedMimeType;
            return this;
        }

        public Builder addProperty(String key, Object value) {
            if (key != null && value != null) {
                this.documentProperties.put(key, value);
            }
            return this;
        }

        public Builder documentProperties(Map<String, Object> properties) {
            if (properties != null) {
                this.documentProperties.putAll(properties);
            }
            return this;
        }

        public ParsedDocument build() {
            return new ParsedDocument(this);
        }
    }
}
