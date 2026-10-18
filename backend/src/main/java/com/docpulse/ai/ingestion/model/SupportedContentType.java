package com.docpulse.ai.ingestion.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Supported MIME types and file extensions for document ingestion in DocPulse AI.
 */
public enum SupportedContentType {

    PDF("application/pdf", ".pdf", Set.of("application/pdf", "application/x-pdf")),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx",
            Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
    DOC("application/msword", ".doc", Set.of("application/msword")),
    TEXT_PLAIN("text/plain", ".txt", Set.of("text/plain")),
    MARKDOWN("text/markdown", ".md", Set.of("text/markdown", "text/x-markdown")),
    CSV("text/csv", ".csv", Set.of("text/csv", "application/csv"));

    private final String canonicalMimeType;
    private final String primaryExtension;
    private final Set<String> matchingMimeTypes;

    SupportedContentType(String canonicalMimeType, String primaryExtension, Set<String> matchingMimeTypes) {
        this.canonicalMimeType = canonicalMimeType;
        this.primaryExtension = primaryExtension;
        this.matchingMimeTypes = matchingMimeTypes;
    }

    private static final Set<String> ALL_SUPPORTED_MIME_TYPES = Arrays.stream(values())
            .flatMap(type -> type.matchingMimeTypes.stream())
            .collect(Collectors.toUnmodifiableSet());

    /**
     * Checks if the given MIME type is supported for ingestion.
     */
    public static boolean isSupported(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return false;
        }
        String cleanMime = cleanMime(mimeType);
        return ALL_SUPPORTED_MIME_TYPES.contains(cleanMime);
    }

    /**
     * Resolves the SupportedContentType from a detected MIME type string.
     */
    public static Optional<SupportedContentType> fromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return Optional.empty();
        }
        String cleanMime = cleanMime(mimeType);
        for (SupportedContentType type : values()) {
            if (type.matchingMimeTypes.contains(cleanMime)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /**
     * Resolves the SupportedContentType from a file name extension.
     */
    public static Optional<SupportedContentType> fromFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            return Optional.empty();
        }
        String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        for (SupportedContentType type : values()) {
            if (type.primaryExtension.equalsIgnoreCase(ext)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    public static Set<String> getAllSupportedMimeTypes() {
        return ALL_SUPPORTED_MIME_TYPES;
    }

    public static String getAllowedFormatsDescription() {
        return "PDF (.pdf), Word (.docx, .doc), Plain Text (.txt), Markdown (.md), CSV (.csv)";
    }

    private static String cleanMime(String mimeType) {
        int semicolon = mimeType.indexOf(';');
        String base = semicolon > 0 ? mimeType.substring(0, semicolon) : mimeType;
        return base.trim().toLowerCase(Locale.ROOT);
    }

    public String getCanonicalMimeType() {
        return canonicalMimeType;
    }

    public String getPrimaryExtension() {
        return primaryExtension;
    }

    public Set<String> getMatchingMimeTypes() {
        return Collections.unmodifiableSet(matchingMimeTypes);
    }
}
