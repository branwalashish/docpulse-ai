package com.docpulse.ai.common.exception;

/**
 * Base exception thrown when an error occurs while parsing or extracting text from a document.
 */
public class DocumentParsingException extends DocPulseException {

    private final String filename;

    public DocumentParsingException(String filename, String message) {
        super(formatMessage(filename, message));
        this.filename = filename;
    }

    public DocumentParsingException(String filename, String message, Throwable cause) {
        super(formatMessage(filename, message), cause);
        this.filename = filename;
    }

    private static String formatMessage(String filename, String message) {
        return (filename != null && !filename.isBlank())
                ? "Failed to parse document '" + filename + "': " + message
                : "Failed to parse document: " + message;
    }

    public String getFilename() {
        return filename;
    }
}
