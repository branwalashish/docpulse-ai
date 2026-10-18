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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VectorStoreIntegrationTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private PgVectorStore vectorStore;

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
}

