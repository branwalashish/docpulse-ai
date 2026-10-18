-- ============================================================================
-- DocPulse AI - Docker PostgreSQL + pgvector Initialization Script
-- ============================================================================

-- 1. Enable Required Extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 2. Create the Vector Store Table
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    embedding vector(384)
);

-- 3. HNSW Cosine Similarity Index
CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx 
ON vector_store USING hnsw (embedding vector_cosine_ops);

-- 4. Multi-Tenant Isolation Index
CREATE INDEX IF NOT EXISTS idx_vector_store_tenant_id 
ON vector_store USING btree ((metadata->>'tenant_id'));

-- 5. Document-level Index
CREATE INDEX IF NOT EXISTS idx_vector_store_document_id 
ON vector_store USING btree ((metadata->>'document_id'));

-- 6. GIN Index on Metadata
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_gin 
ON vector_store USING gin (metadata jsonb_path_ops);
