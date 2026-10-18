package com.docpulse.ai.common.exception;

/**
 * Thrown when an uploaded document's detected MIME type is outside the allowed whitelist.
 */
public class UnsupportedDocumentTypeException extends DocumentParsingException {

    private final String detectedMimeType;

    public UnsupportedDocumentTypeException(String filename, String detectedMimeType) {
        super(filename, "Detected MIME type '" + detectedMimeType + "' is not supported. Allowed formats: PDF, DOCX, TXT, MD");
        this.detectedMimeType = detectedMimeType;
    }

    public UnsupportedDocumentTypeException(String filename, String detectedMimeType, String message) {
        super(filename, message);
        this.detectedMimeType = detectedMimeType;
    }

    public String getDetectedMimeType() {
        return detectedMimeType;
    }
}
