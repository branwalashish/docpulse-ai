package com.docpulse.ai.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentParsingExceptionTest {

    @Test
    @DisplayName("DocumentParsingException should preserve filename, cause, and format message cleanly")
    void testDocumentParsingException() {
        RuntimeException cause = new RuntimeException("Corrupted stream");
        DocumentParsingException ex = new DocumentParsingException("report.pdf", "Unable to extract text", cause);

        assertThat(ex.getFilename()).isEqualTo("report.pdf");
        assertThat(ex.getMessage()).contains("report.pdf").contains("Unable to extract text");
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex).isInstanceOf(DocPulseException.class);
    }

    @Test
    @DisplayName("EncryptedDocumentException should identify password-protected documents")
    void testEncryptedDocumentException() {
        EncryptedDocumentException ex = new EncryptedDocumentException("confidential_payroll.pdf");

        assertThat(ex.getFilename()).isEqualTo("confidential_payroll.pdf");
        assertThat(ex.getMessage()).contains("password-protected or encrypted");
        assertThat(ex).isInstanceOf(DocumentParsingException.class);
    }

    @Test
    @DisplayName("EmptyDocumentException should identify empty or blank content")
    void testEmptyDocumentException() {
        EmptyDocumentException ex = new EmptyDocumentException("blank_page.docx");

        assertThat(ex.getFilename()).isEqualTo("blank_page.docx");
        assertThat(ex.getMessage()).contains("no readable text or is empty");
        assertThat(ex).isInstanceOf(DocumentParsingException.class);
    }

    @Test
    @DisplayName("UnsupportedDocumentTypeException should retain detected MIME type and filename")
    void testUnsupportedDocumentTypeException() {
        UnsupportedDocumentTypeException ex = new UnsupportedDocumentTypeException("archive.zip", "application/zip");

        assertThat(ex.getFilename()).isEqualTo("archive.zip");
        assertThat(ex.getDetectedMimeType()).isEqualTo("application/zip");
        assertThat(ex.getMessage()).contains("application/zip").contains("PDF, DOCX, TXT, MD");
        assertThat(ex).isInstanceOf(DocumentParsingException.class);
    }
}
