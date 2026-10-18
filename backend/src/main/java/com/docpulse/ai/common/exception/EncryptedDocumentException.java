package com.docpulse.ai.common.exception;

/**
 * Thrown when attempting to parse a password-protected or encrypted document.
 */
public class EncryptedDocumentException extends DocumentParsingException {

    public EncryptedDocumentException(String filename) {
        super(filename, "Document is password-protected or encrypted and cannot be parsed");
    }

    public EncryptedDocumentException(String filename, Throwable cause) {
        super(filename, "Document is password-protected or encrypted and cannot be parsed", cause);
    }
}
