-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- The Spring AI PGVector store will automatically create the vector_store table when initialize-schema is true.
-- You can verify or create the custom HNSW index manually if needed:
-- CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx 
-- ON vector_store USING hnsw (embedding vector_cosine_ops);

