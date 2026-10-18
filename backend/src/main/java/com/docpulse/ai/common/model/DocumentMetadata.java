package com.docpulse.ai.common.model;

import com.docpulse.ai.common.constant.DocumentMetadataKey;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain model representing the unified metadata contract for DocPulse AI vector chunks.
 * Enforces multi-tenant isolation attributes and data integrity before persistence.
 */
public class DocumentMetadata {

    private final String tenantId;
    private final String documentId;
    private final String documentName;
    private final String chunkId;
    private final AccessTier accessTier;
    private final Instant createdAt;
    private final Integer chunkIndex;
    private final Integer totalChunks;
    private final String contentType;
    private final Map<String, Object> additionalAttributes;

    private DocumentMetadata(Builder builder) {
        this.tenantId = builder.tenantId;
        this.documentId = builder.documentId != null ? builder.documentId : UUID.randomUUID().toString();
        this.documentName = builder.documentName;
        this.chunkId = builder.chunkId != null ? builder.chunkId : UUID.randomUUID().toString();
        this.accessTier = builder.accessTier != null ? builder.accessTier : AccessTier.DEFAULT;
        this.createdAt = builder.createdAt != null ? builder.createdAt : Instant.now();
        this.chunkIndex = builder.chunkIndex;
        this.totalChunks = builder.totalChunks;
        this.contentType = builder.contentType;
        this.additionalAttributes = builder.additionalAttributes != null
                ? Map.copyOf(builder.additionalAttributes)
                : Collections.emptyMap();
        validate();
    }

    /**
     * Validates that all required multi-tenant contract fields are present and non-blank.
     */
    public void validate() {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenant_id is required and cannot be blank");
        }
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("document_id is required and cannot be blank");
        }
        if (documentName == null || documentName.isBlank()) {
            throw new IllegalArgumentException("document_name is required and cannot be blank");
        }
        if (chunkId == null || chunkId.isBlank()) {
            throw new IllegalArgumentException("chunk_id is required and cannot be blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("created_at timestamp cannot be null");
        }
    }

    /**
     * Converts this metadata contract to a Map<String, Object> formatted for Spring AI Document metadata.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(DocumentMetadataKey.TENANT_ID, tenantId);
        map.put(DocumentMetadataKey.DOCUMENT_ID, documentId);
        map.put(DocumentMetadataKey.DOCUMENT_NAME, documentName);
        map.put(DocumentMetadataKey.CHUNK_ID, chunkId);
        map.put(DocumentMetadataKey.ACCESS_TIER, accessTier.name());
        map.put(DocumentMetadataKey.CREATED_AT, createdAt.toString());

        if (chunkIndex != null) {
            map.put(DocumentMetadataKey.CHUNK_INDEX, chunkIndex);
        }
        if (totalChunks != null) {
            map.put(DocumentMetadataKey.TOTAL_CHUNKS, totalChunks);
        }
        if (contentType != null) {
            map.put(DocumentMetadataKey.CONTENT_TYPE, contentType);
        }

        // Include any additional attributes without overwriting contract keys
        additionalAttributes.forEach((k, v) -> {
            if (!map.containsKey(k) && v != null) {
                map.put(k, v);
            }
        });

        return Collections.unmodifiableMap(map);
    }

    /**
     * Reconstructs a DocumentMetadata object from a Spring AI metadata Map.
     */
    public static DocumentMetadata fromMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            throw new IllegalArgumentException("Metadata map cannot be null or empty");
        }

        String tenantId = getString(map, DocumentMetadataKey.TENANT_ID);
        String documentId = getString(map, DocumentMetadataKey.DOCUMENT_ID);
        String documentName = getString(map, DocumentMetadataKey.DOCUMENT_NAME);
        String chunkId = getString(map, DocumentMetadataKey.CHUNK_ID);
        String accessTierStr = getString(map, DocumentMetadataKey.ACCESS_TIER);
        String createdAtStr = getString(map, DocumentMetadataKey.CREATED_AT);

        Instant createdAt = createdAtStr != null ? parseInstant(createdAtStr) : Instant.now();
        Integer chunkIndex = getInteger(map, DocumentMetadataKey.CHUNK_INDEX);
        Integer totalChunks = getInteger(map, DocumentMetadataKey.TOTAL_CHUNKS);
        String contentType = getString(map, DocumentMetadataKey.CONTENT_TYPE);

        Builder builder = builder()
                .tenantId(tenantId)
                .documentId(documentId)
                .documentName(documentName)
                .chunkId(chunkId)
                .accessTier(AccessTier.fromString(accessTierStr))
                .createdAt(createdAt)
                .chunkIndex(chunkIndex)
                .totalChunks(totalChunks)
                .contentType(contentType);

        // Add remaining attributes
        map.forEach((k, v) -> {
            if (!isStandardKey(k)) {
                builder.addAttribute(k, v);
            }
        });

        return builder.build();
    }

    private static boolean isStandardKey(String key) {
        return DocumentMetadataKey.TENANT_ID.equals(key) ||
                DocumentMetadataKey.DOCUMENT_ID.equals(key) ||
                DocumentMetadataKey.DOCUMENT_NAME.equals(key) ||
                DocumentMetadataKey.CHUNK_ID.equals(key) ||
                DocumentMetadataKey.ACCESS_TIER.equals(key) ||
                DocumentMetadataKey.CREATED_AT.equals(key) ||
                DocumentMetadataKey.CHUNK_INDEX.equals(key) ||
                DocumentMetadataKey.TOTAL_CHUNKS.equals(key) ||
                DocumentMetadataKey.CONTENT_TYPE.equals(key);
    }

    private static String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private static Integer getInteger(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number num) {
            return num.intValue();
        } else if (val instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Instant parseInstant(String instantStr) {
        try {
            return Instant.parse(instantStr);
        } catch (Exception e) {
            return Instant.now();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getChunkId() {
        return chunkId;
    }

    public AccessTier getAccessTier() {
        return accessTier;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public Integer getTotalChunks() {
        return totalChunks;
    }

    public String getContentType() {
        return contentType;
    }

    public Map<String, Object> getAdditionalAttributes() {
        return additionalAttributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocumentMetadata that)) return false;
        return Objects.equals(tenantId, that.tenantId) &&
                Objects.equals(documentId, that.documentId) &&
                Objects.equals(documentName, that.documentName) &&
                Objects.equals(chunkId, that.chunkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, documentId, documentName, chunkId);
    }

    @Override
    public String toString() {
        return "DocumentMetadata{" +
                "tenantId='" + tenantId + '\'' +
                ", documentId='" + documentId + '\'' +
                ", documentName='" + documentName + '\'' +
                ", chunkId='" + chunkId + '\'' +
                ", accessTier=" + accessTier +
                ", createdAt=" + createdAt +
                ", chunkIndex=" + chunkIndex +
                ", totalChunks=" + totalChunks +
                '}';
    }

    public static class Builder {
        private String tenantId;
        private String documentId;
        private String documentName;
        private String chunkId;
        private AccessTier accessTier = AccessTier.DEFAULT;
        private Instant createdAt = Instant.now();
        private Integer chunkIndex;
        private Integer totalChunks;
        private String contentType;
        private Map<String, Object> additionalAttributes = new HashMap<>();

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder documentId(String documentId) {
            this.documentId = documentId;
            return this;
        }

        public Builder documentName(String documentName) {
            this.documentName = documentName;
            return this;
        }

        public Builder chunkId(String chunkId) {
            this.chunkId = chunkId;
            return this;
        }

        public Builder accessTier(AccessTier accessTier) {
            if (accessTier != null) {
                this.accessTier = accessTier;
            }
            return this;
        }

        public Builder accessTier(String accessTier) {
            if (accessTier != null) {
                this.accessTier = AccessTier.fromString(accessTier);
            }
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            if (createdAt != null) {
                this.createdAt = createdAt;
            }
            return this;
        }

        public Builder chunkIndex(Integer chunkIndex) {
            this.chunkIndex = chunkIndex;
            return this;
        }

        public Builder totalChunks(Integer totalChunks) {
            this.totalChunks = totalChunks;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder addAttribute(String key, Object value) {
            if (key != null && value != null) {
                this.additionalAttributes.put(key, value);
            }
            return this;
        }

        public Builder additionalAttributes(Map<String, Object> attributes) {
            if (attributes != null) {
                this.additionalAttributes.putAll(attributes);
            }
            return this;
        }

        public DocumentMetadata build() {
            return new DocumentMetadata(this);
        }
    }
}
