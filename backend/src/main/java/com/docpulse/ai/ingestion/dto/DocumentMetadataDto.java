package com.docpulse.ai.ingestion.dto;

import com.docpulse.ai.common.model.AccessTier;
import com.docpulse.ai.common.model.DocumentMetadata;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object for document and chunk metadata in ingestion and search APIs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentMetadataDto {

    @NotBlank(message = "tenant_id is required and cannot be blank")
    @JsonProperty("tenant_id")
    private String tenantId;

    @NotBlank(message = "document_id is required and cannot be blank")
    @JsonProperty("document_id")
    private String documentId;

    @NotBlank(message = "document_name is required and cannot be blank")
    @JsonProperty("document_name")
    private String documentName;

    @JsonProperty("chunk_id")
    private String chunkId;

    @JsonProperty("access_tier")
    private String accessTier;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("chunk_index")
    private Integer chunkIndex;

    @JsonProperty("total_chunks")
    private Integer totalChunks;

    @JsonProperty("content_type")
    private String contentType;

    public DocumentMetadataDto() {
    }

    public DocumentMetadataDto(String tenantId, String documentId, String documentName, String chunkId,
                               String accessTier, Instant createdAt, Integer chunkIndex,
                               Integer totalChunks, String contentType) {
        this.tenantId = tenantId;
        this.documentId = documentId;
        this.documentName = documentName;
        this.chunkId = chunkId;
        this.accessTier = accessTier;
        this.createdAt = createdAt;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.contentType = contentType;
    }

    /**
     * Converts this DTO to a validated DocumentMetadata domain model.
     */
    public DocumentMetadata toDomain() {
        return DocumentMetadata.builder()
                .tenantId(tenantId)
                .documentId(documentId != null ? documentId : UUID.randomUUID().toString())
                .documentName(documentName)
                .chunkId(chunkId != null ? chunkId : UUID.randomUUID().toString())
                .accessTier(AccessTier.fromString(accessTier))
                .createdAt(createdAt != null ? createdAt : Instant.now())
                .chunkIndex(chunkIndex)
                .totalChunks(totalChunks)
                .contentType(contentType)
                .build();
    }

    /**
     * Creates a DTO from a DocumentMetadata domain model.
     */
    public static DocumentMetadataDto fromDomain(DocumentMetadata domain) {
        if (domain == null) {
            return null;
        }
        return new DocumentMetadataDto(
                domain.getTenantId(),
                domain.getDocumentId(),
                domain.getDocumentName(),
                domain.getChunkId(),
                domain.getAccessTier().name(),
                domain.getCreatedAt(),
                domain.getChunkIndex(),
                domain.getTotalChunks(),
                domain.getContentType()
        );
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getAccessTier() {
        return accessTier;
    }

    public void setAccessTier(String accessTier) {
        this.accessTier = accessTier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
