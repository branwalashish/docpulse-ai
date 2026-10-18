package com.docpulse.ai.ingestion.dto;

import com.docpulse.ai.ingestion.model.ParsedDocument;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Data Transfer Object for parsed document representations in API contracts.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParsedDocumentDto {

    @JsonProperty("document_id")
    private String documentId;

    @JsonProperty("document_name")
    private String documentName;

    @JsonProperty("detected_mime_type")
    private String detectedMimeType;

    @JsonProperty("character_count")
    private int characterCount;

    @JsonProperty("word_count")
    private int wordCount;

    @JsonProperty("properties")
    private Map<String, Object> documentProperties;

    public ParsedDocumentDto() {
    }

    public ParsedDocumentDto(String documentId, String documentName, String detectedMimeType,
                             int characterCount, int wordCount, Map<String, Object> documentProperties) {
        this.documentId = documentId;
        this.documentName = documentName;
        this.detectedMimeType = detectedMimeType;
        this.characterCount = characterCount;
        this.wordCount = wordCount;
        this.documentProperties = documentProperties;
    }

    public static ParsedDocumentDto fromDomain(ParsedDocument domain) {
        if (domain == null) {
            return null;
        }
        return new ParsedDocumentDto(
                domain.getDocumentId(),
                domain.getDocumentName(),
                domain.getDetectedMimeType(),
                domain.getCharacterCount(),
                domain.getWordCount(),
                domain.getDocumentProperties()
        );
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDetectedMimeType() {
        return detectedMimeType;
    }

    public void setDetectedMimeType(String detectedMimeType) {
        this.detectedMimeType = detectedMimeType;
    }

    public int getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(int characterCount) {
        this.characterCount = characterCount;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public Map<String, Object> getDocumentProperties() {
        return documentProperties;
    }

    public void setDocumentProperties(Map<String, Object> documentProperties) {
        this.documentProperties = documentProperties;
    }
}
