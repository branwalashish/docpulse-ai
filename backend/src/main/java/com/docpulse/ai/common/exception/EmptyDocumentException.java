package com.docpulse.ai.common.exception;

/**
 * Thrown when an uploaded document contains 0 bytes or yields only whitespace upon parsing.
 */
public class EmptyDocumentException extends DocumentParsingException {

    public EmptyDocumentException(String filename) {
        super(filename, "Document contains no readable text or is empty");
    }

    public EmptyDocumentException(String filename, String detail) {
        super(filename, detail != null ? detail : "Document contains no readable text or is empty");
    }
}
