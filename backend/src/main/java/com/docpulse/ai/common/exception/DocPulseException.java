package com.docpulse.ai.common.exception;

/**
 * Base abstract exception for all custom domain exceptions in DocPulse AI.
 */
public abstract class DocPulseException extends RuntimeException {

    protected DocPulseException(String message) {
        super(message);
    }

    protected DocPulseException(String message, Throwable cause) {
        super(message, cause);
    }
}
