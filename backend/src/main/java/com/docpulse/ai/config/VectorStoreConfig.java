package com.docpulse.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/**
 * VectorStore and Embedding configuration for DocPulse AI.
 * Sets up the PgVectorStore bean with HNSW cosine distance indexing (384 dimensions)
 * and configures the HuggingFace bge-small-en-v1.5 EmbeddingModel.
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(
            @Value("${spring.ai.embedding.huggingface.base-url:https://api-inference.huggingface.co/pipeline/feature-extraction/}") String baseUrl,
            @Value("${spring.ai.embedding.huggingface.model:BAAI/bge-small-en-v1.5}") String model,
            @Value("${spring.ai.embedding.huggingface.api-key:${HUGGINGFACE_API_KEY:}}") String apiKey,
            @Value("${spring.ai.embedding.huggingface.dimensions:384}") int dimensions,
            RestClient.Builder restClientBuilder) {
        log.info("Configuring primary EmbeddingModel using HuggingFace model '{}' ({} dimensions)", model, dimensions);
        return new HuggingFaceEmbeddingModel(baseUrl, model, apiKey, dimensions, restClientBuilder);
    }

    @Bean
    @Primary
    public PgVectorStore vectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel,
            @Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}") String tableName,
            @Value("${spring.ai.vectorstore.pgvector.dimensions:384}") int dimensions) {
        log.info("Configuring PgVectorStore: table='{}', dimensions={}, indexType=HNSW, distanceType=COSINE_DISTANCE",
                tableName, dimensions);

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(tableName)
                .dimensions(dimensions)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(false) // Schema and indexes are initialized by schema.sql
                .build();
    }
}

