package com.docpulse.ai.common.constant;

/**
 * Metadata key constants for DocPulse AI vector store chunks.
 * Enforces schema consistency and prevents magic string duplication across ingestion and retrieval.
 */
public final class DocumentMetadataKey {

    private DocumentMetadataKey() {
        // Prevent instantiation
    }

    /**
     * Mandatory tenant partition key (e.g., 'tenant-engineering', 'tenant-hr', 'tenant-legal').
     */
    public static final String TENANT_ID = "tenant_id";

    /**
     * Unique identifier for the parent document.
     */
    public static final String DOCUMENT_ID = "document_id";

    /**
     * Source document filename (e.g., 'employee_handbook.pdf').
     */
    public static final String DOCUMENT_NAME = "document_name";

    /**
     * Unique identifier for the chunk.
     */
    public static final String CHUNK_ID = "chunk_id";

    /**
     * Access control classification (e.g., 'DEFAULT', 'INTERNAL', 'CONFIDENTIAL', 'RESTRICTED').
     */
    public static final String ACCESS_TIER = "access_tier";

    /**
     * ISO-8601 creation timestamp.
     */
    public static final String CREATED_AT = "created_at";

    /**
     * Sequential zero-based index of this chunk within the parent document.
     */
    public static final String CHUNK_INDEX = "chunk_index";

    /**
     * Total number of chunks extracted from the parent document.
     */
    public static final String TOTAL_CHUNKS = "total_chunks";

    /**
     * MIME type of the original file (e.g., 'application/pdf', 'text/plain').
     */
    public static final String CONTENT_TYPE = "content_type";
}
