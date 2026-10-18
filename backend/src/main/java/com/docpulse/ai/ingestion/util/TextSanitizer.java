package com.docpulse.ai.ingestion.util;

import java.util.regex.Pattern;

/**
 * Utility for sanitizing and normalizing raw document text extracted by Apache Tika.
 * Ensures text is clean, free of PostgreSQL-incompatible characters (e.g. null bytes \u0000),
 * and optimized for token-aware chunking and embedding generation.
 */
public final class TextSanitizer {

    private TextSanitizer() {
        // Prevent instantiation
    }

    // Pattern for PostgreSQL-incompatible null characters
    private static final Pattern NULL_CHAR_PATTERN = Pattern.compile("\u0000");

    // Pattern for ASCII control characters excluding \t (0x09), \n (0x0A), and \r (0x0D)
    private static final Pattern CONTROL_CHARS_PATTERN = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    // Pattern for horizontal spaces (spaces and tabs) repeating 2 or more times
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("[^\\S\\r\\n]+");

    // Pattern for 3 or more consecutive newlines (preserves paragraph breaks as \n\n)
    private static final Pattern EXCESSIVE_NEWLINES_PATTERN = Pattern.compile("\n{3,}");

    /**
     * Sanitizes raw text extracted from documents:
     * 1. Strips PostgreSQL-incompatible null bytes (\u0000).
     * 2. Strips illegal control characters while preserving valid formatting (\t, \n, \r).
     * 3. Normalizes line separators (\r\n and \r to \n).
     * 4. Converts non-breaking spaces (\u00A0) to standard spaces.
     * 5. Collapses redundant horizontal spaces.
     * 6. Collapses excessive blank lines to standard paragraph breaks (\n\n).
     * 7. Trims outer leading and trailing whitespace.
     *
     * @param rawText Text extracted from documents.
     * @return Sanitized, PostgreSQL-safe, and token-optimized plaintext.
     */
    public static String sanitize(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return "";
        }

        // 1. Remove null bytes immediately (critical for Postgres TEXT and JSONB)
        String cleaned = NULL_CHAR_PATTERN.matcher(rawText).replaceAll("");

        // 2. Normalize Windows and legacy Mac line breaks to UNIX \n
        cleaned = cleaned.replace("\r\n", "\n").replace('\r', '\n');

        // 3. Replace non-breaking spaces (\u00A0) with standard space
        cleaned = cleaned.replace('\u00A0', ' ');

        // 4. Remove illegal control characters
        cleaned = CONTROL_CHARS_PATTERN.matcher(cleaned).replaceAll("");

        // 5. Collapse multiple horizontal spaces/tabs on the same line into a single space
        cleaned = MULTIPLE_SPACES_PATTERN.matcher(cleaned).replaceAll(" ");

        // 6. Clean spaces around newlines
        cleaned = cleaned.replaceAll(" *\n *", "\n");

        // 7. Collapse 3+ consecutive newlines into 2 (\n\n)
        cleaned = EXCESSIVE_NEWLINES_PATTERN.matcher(cleaned).replaceAll("\n\n");

        // 8. Trim outer whitespace
        return cleaned.trim();
    }

    /**
     * Checks if the given text is empty or blank after sanitization.
     */
    public static boolean isBlank(String text) {
        return text == null || sanitize(text).isEmpty();
    }
}
