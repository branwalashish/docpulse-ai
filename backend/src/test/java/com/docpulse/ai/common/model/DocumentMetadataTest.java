package com.docpulse.ai.common.model;

import com.docpulse.ai.common.constant.DocumentMetadataKey;
import com.docpulse.ai.ingestion.dto.DocumentMetadataDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentMetadataTest {

    @Test
    @DisplayName("Should successfully construct DocumentMetadata and serialize to Spring AI Map")
    void testValidMetadataCreationAndSerialization() {
        String tenantId = "tenant-engineering";
        String documentId = UUID.randomUUID().toString();
        String documentName = "system-architecture.pdf";
        String chunkId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        DocumentMetadata metadata = DocumentMetadata.builder()
                .tenantId(tenantId)
                .documentId(documentId)
                .documentName(documentName)
                .chunkId(chunkId)
                .accessTier(AccessTier.CONFIDENTIAL)
                .createdAt(now)
                .chunkIndex(0)
                .totalChunks(5)
                .contentType("application/pdf")
                .addAttribute("custom_tag", "v1.0")
                .build();

        Map<String, Object> map = metadata.toMap();

        assertThat(map).isNotNull();
        assertThat(map.get(DocumentMetadataKey.TENANT_ID)).isEqualTo(tenantId);
        assertThat(map.get(DocumentMetadataKey.DOCUMENT_ID)).isEqualTo(documentId);
        assertThat(map.get(DocumentMetadataKey.DOCUMENT_NAME)).isEqualTo(documentName);
        assertThat(map.get(DocumentMetadataKey.CHUNK_ID)).isEqualTo(chunkId);
        assertThat(map.get(DocumentMetadataKey.ACCESS_TIER)).isEqualTo("CONFIDENTIAL");
        assertThat(map.get(DocumentMetadataKey.CREATED_AT)).isEqualTo(now.toString());
        assertThat(map.get(DocumentMetadataKey.CHUNK_INDEX)).isEqualTo(0);
        assertThat(map.get(DocumentMetadataKey.TOTAL_CHUNKS)).isEqualTo(5);
        assertThat(map.get(DocumentMetadataKey.CONTENT_TYPE)).isEqualTo("application/pdf");
        assertThat(map.get("custom_tag")).isEqualTo("v1.0");
    }

    @Test
    @DisplayName("Should correctly reconstruct DocumentMetadata from Spring AI Map")
    void testReconstructionFromMap() {
        Map<String, Object> map = Map.of(
                DocumentMetadataKey.TENANT_ID, "tenant-hr",
                DocumentMetadataKey.DOCUMENT_ID, "doc-789",
                DocumentMetadataKey.DOCUMENT_NAME, "benefits-guide.docx",
                DocumentMetadataKey.CHUNK_ID, "chunk-042",
                DocumentMetadataKey.ACCESS_TIER, "INTERNAL",
                DocumentMetadataKey.CREATED_AT, "2026-10-18T10:15:30Z",
                DocumentMetadataKey.CHUNK_INDEX, 2,
                DocumentMetadataKey.TOTAL_CHUNKS, 10
        );

        DocumentMetadata metadata = DocumentMetadata.fromMap(map);

        assertThat(metadata.getTenantId()).isEqualTo("tenant-hr");
        assertThat(metadata.getDocumentId()).isEqualTo("doc-789");
        assertThat(metadata.getDocumentName()).isEqualTo("benefits-guide.docx");
        assertThat(metadata.getChunkId()).isEqualTo("chunk-042");
        assertThat(metadata.getAccessTier()).isEqualTo(AccessTier.INTERNAL);
        assertThat(metadata.getCreatedAt()).isEqualTo(Instant.parse("2026-10-18T10:15:30Z"));
        assertThat(metadata.getChunkIndex()).isEqualTo(2);
        assertThat(metadata.getTotalChunks()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should fail validation when mandatory tenant_id is missing or blank")
    void testMissingTenantIdThrowsException() {
        assertThatThrownBy(() -> DocumentMetadata.builder()
                .tenantId("")
                .documentId("doc-1")
                .documentName("test.pdf")
                .chunkId("chunk-1")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant_id is required");

        assertThatThrownBy(() -> DocumentMetadata.builder()
                .tenantId(null)
                .documentId("doc-1")
                .documentName("test.pdf")
                .chunkId("chunk-1")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant_id is required");
    }

    @Test
    @DisplayName("Should fail validation when document_name is missing or blank")
    void testMissingDocumentNameThrowsException() {
        assertThatThrownBy(() -> DocumentMetadata.builder()
                .tenantId("tenant-finance")
                .documentId("doc-1")
                .documentName("   ")
                .chunkId("chunk-1")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("document_name is required");
    }

    @Test
    @DisplayName("Should resolve AccessTier safely and fallback to DEFAULT on invalid input")
    void testAccessTierResolution() {
        assertThat(AccessTier.fromString("CONFIDENTIAL")).isEqualTo(AccessTier.CONFIDENTIAL);
        assertThat(AccessTier.fromString("restricted")).isEqualTo(AccessTier.RESTRICTED);
        assertThat(AccessTier.fromString("unknown_tier")).isEqualTo(AccessTier.DEFAULT);
        assertThat(AccessTier.fromString(null)).isEqualTo(AccessTier.DEFAULT);
    }

    @Test
    @DisplayName("Should convert between DocumentMetadata domain and DocumentMetadataDto")
    void testDtoRoundTrip() {
        DocumentMetadata domain = DocumentMetadata.builder()
                .tenantId("tenant-legal")
                .documentId("doc-legal-1")
                .documentName("terms-of-service.pdf")
                .chunkId("chunk-legal-1")
                .accessTier(AccessTier.RESTRICTED)
                .chunkIndex(1)
                .totalChunks(3)
                .contentType("application/pdf")
                .build();

        DocumentMetadataDto dto = DocumentMetadataDto.fromDomain(domain);
        assertThat(dto).isNotNull();
        assertThat(dto.getTenantId()).isEqualTo("tenant-legal");
        assertThat(dto.getDocumentId()).isEqualTo("doc-legal-1");
        assertThat(dto.getAccessTier()).isEqualTo("RESTRICTED");

        DocumentMetadata roundTripped = dto.toDomain();
        assertThat(roundTripped.getTenantId()).isEqualTo(domain.getTenantId());
        assertThat(roundTripped.getDocumentId()).isEqualTo(domain.getDocumentId());
        assertThat(roundTripped.getAccessTier()).isEqualTo(domain.getAccessTier());
    }
}
