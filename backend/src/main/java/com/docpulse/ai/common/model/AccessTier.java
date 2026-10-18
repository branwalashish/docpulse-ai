package com.docpulse.ai.common.model;

/**
 * Access tier classification for documents and chunks.
 */
public enum AccessTier {
    DEFAULT,
    INTERNAL,
    CONFIDENTIAL,
    RESTRICTED;

    /**
     * Resolves an AccessTier from a string safely, defaulting to DEFAULT if invalid or null.
     */
    public static AccessTier fromString(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        try {
            return AccessTier.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }
}
