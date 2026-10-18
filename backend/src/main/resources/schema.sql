-- ============================================================================
-- DocPulse AI - PostgreSQL + pgvector Schema Initialization
-- Compatible with PostgreSQL 16+ and pgvector 0.7+
-- ============================================================================

-- 1. Enable Required Extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 2. Create the Vector Store Table
-- Schema matches Spring AI's PgVectorStore contract
-- Dimension: 384 (matches BAAI/bge-small-en-v1.5)
CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    embedding vector(384)
);

-- 3. HNSW Cosine Similarity Index
-- HNSW provides sub-millisecond approximate nearest neighbor retrieval
-- vector_cosine_ops is required for cosine similarity queries
CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx 
ON vector_store USING hnsw (embedding vector_cosine_ops);

-- 4. Multi-Tenant Isolation Index
-- Essential for RAG metadata filtering (tenant_id == currentTenant)
-- B-Tree on extracted tenant_id provides fast index scans within tenant boundaries
CREATE INDEX IF NOT EXISTS idx_vector_store_tenant_id 
ON vector_store USING btree ((metadata->>'tenant_id'));

-- 5. Document-level Index
-- Accelerates document deletion, updates, and document-level aggregations
CREATE INDEX IF NOT EXISTS idx_vector_store_document_id 
ON vector_store USING btree ((metadata->>'document_id'));

-- 6. GIN Index on Metadata
-- Supports general JSONB containment queries
CREATE INDEX IF NOT EXISTS idx_vector_store_metadata_gin 
ON vector_store USING gin (metadata jsonb_path_ops);
