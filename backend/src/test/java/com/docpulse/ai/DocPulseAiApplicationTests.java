package com.docpulse.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DocPulseAiApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        assertThat(jdbcTemplate).isNotNull();
    }

    @Test
    @DisplayName("Verify pgvector extension, vector_store table, and HNSW & tenant indexes exist")
    void verifyVectorSchemaAndIndexes() {
        // 1. Verify vector extension
        Integer extensionCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_extension WHERE extname = 'vector'",
            Integer.class
        );
        assertThat(extensionCount).isNotNull().isGreaterThan(0);

        // 2. Verify vector_store table exists
        Integer tableCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_name = 'vector_store'",
            Integer.class
        );
        assertThat(tableCount).isNotNull().isEqualTo(1);

        // 3. Verify HNSW index exists
        Integer hnswIndexCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_indexes WHERE tablename = 'vector_store' AND indexname = 'vector_store_hnsw_idx'",
            Integer.class
        );
        assertThat(hnswIndexCount).isNotNull().isEqualTo(1);

        // 4. Verify multi-tenant index exists
        Integer tenantIndexCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM pg_indexes WHERE tablename = 'vector_store' AND indexname = 'idx_vector_store_tenant_id'",
            Integer.class
        );
        assertThat(tenantIndexCount).isNotNull().isEqualTo(1);
    }
}

