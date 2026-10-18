package com.docpulse.ai.vectorstore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.docpulse.ai.common.model.AccessTier;
import com.docpulse.ai.common.model.DocumentMetadata;
import com.docpulse.ai.ingestion.model.ParsedDocument;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VectorStoreIntegrationTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private PgVectorStore vectorStore;

    @Autowired
    private com.docpulse.ai.ingestion.service.DocumentParsingService parsingService;

    @Test
    @DisplayName("Verify EmbeddingModel produces 384-dimensional unit vectors")
    void testEmbeddingModelDimensions() {
        assertThat(embeddingModel).isNotNull();
        assertThat(embeddingModel.dimensions()).isEqualTo(384);

        float[] embedding = embeddingModel.embed("Enterprise multi-tenant RAG architecture");
        assertThat(embedding).isNotNull();
        assertThat(embedding.length).isEqualTo(384);

        // Verify vector is normalized (unit vector length ~ 1.0)
        double norm = 0.0;
        for (float val : embedding) {
            norm += val * val;
        }
        assertThat(Math.sqrt(norm)).isBetween(0.99, 1.01);
    }

    @Test
    @DisplayName("Verify PgVectorStore bean is created and can persist and retrieve documents")
    void testVectorStorePersistenceAndRetrieval() {
        assertThat(vectorStore).isNotNull();

        String testContent = "DocPulse AI uses Spring AI with PGVector and HNSW cosine indexing.";
        DocumentMetadata metadata = DocumentMetadata.builder()
                .tenantId("test-tenant-day2")
                .documentId("doc-12345")
                .documentName("architecture-day2.pdf")
                .chunkId("chunk-001")
                .accessTier(AccessTier.DEFAULT)
                .build();

        Document doc = new Document(testContent, metadata.toMap());

        // 1. Add document to vector store
        vectorStore.add(List.of(doc));

        // 2. Perform similarity search
        SearchRequest searchRequest = SearchRequest.builder()
                .query("What does DocPulse AI use for vector indexing?")
                .topK(2)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        assertThat(results).isNotEmpty();
        Document retrieved = results.get(0);
        assertThat(retrieved.getMetadata()).containsKey("tenant_id");
        assertThat(retrieved.getMetadata().get("tenant_id")).isEqualTo("test-tenant-day2");

        // 3. Clean up test document
        vectorStore.delete(List.of(doc.getId()));
    }

    @Test
    @DisplayName("Verify End-to-End pipeline across Day 1 (Postgres/Virtual Threads), Day 2 (Vector Store/Metadata), and Day 3 (Document Parsing/Sanitization)")
    void testEndToEndDay1Day2Day3Pipeline() {
        assertThat(parsingService).isNotNull();
        assertThat(vectorStore).isNotNull();

        // 1. DAY 3: Parse and sanitize raw markdown document with potential null bytes and Windows line endings
        String rawMarkdown = "# Tenant Onboarding Policy\r\n\r\n"
                + "DocPulse AI enforces strict multi-tenant isolation at the database vector level.\r\n"
                + "Every chunk is tagged with tenant_id and access_tier.\0\r\n";

        org.springframework.mock.web.MockMultipartFile multipartFile = new org.springframework.mock.web.MockMultipartFile(
                "file",
                "tenant_policy.md",
                "text/markdown",
                rawMarkdown.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        ParsedDocument parsed = parsingService.parse(multipartFile);

        assertThat(parsed).isNotNull();
        assertThat(parsed.getRawText()).doesNotContain("\u0000");
        assertThat(parsed.getRawText()).contains("Tenant Onboarding Policy");
        assertThat(parsed.getWordCount()).isGreaterThan(0);

        // 2. DAY 2: Attach multi-tenant metadata contract
        DocumentMetadata metadata = DocumentMetadata.builder()
                .tenantId("tenant-finance-e2e")
                .documentId("doc-fin-999")
                .documentName("tenant_policy.md")
                .chunkId("chunk-0")
                .accessTier(AccessTier.CONFIDENTIAL)
                .build();

        Document document = new Document(parsed.getRawText(), metadata.toMap());

        // 3. DAY 1 & 2: Ingest into PostgreSQL pgvector table
        vectorStore.add(List.of(document));

        try {
            // 4. DAY 2: Query vector store with semantic search
            SearchRequest query = SearchRequest.builder()
                    .query("How does DocPulse AI isolate tenants at the vector level?")
                    .topK(3)
                    .build();

            List<Document> matched = vectorStore.similaritySearch(query);

            assertThat(matched).isNotEmpty();
            Document topResult = matched.get(0);
            assertThat(topResult.getMetadata().get("tenant_id")).isEqualTo("tenant-finance-e2e");
            assertThat(topResult.getMetadata().get("access_tier")).isEqualTo(AccessTier.CONFIDENTIAL.name());
            assertThat(topResult.getText()).contains("Tenant Onboarding Policy");
        } finally {
            // 5. Cleanup
            vectorStore.delete(List.of(document.getId()));
        }
    }
}

