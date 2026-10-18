package com.docpulse.ai.ingestion.service;

import com.docpulse.ai.common.exception.EmptyDocumentException;
import com.docpulse.ai.common.exception.EncryptedDocumentException;
import com.docpulse.ai.common.exception.UnsupportedDocumentTypeException;
import com.docpulse.ai.ingestion.model.ParsedDocument;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentParsingServiceTest {

    private DocumentParsingService parsingService;

    @BeforeEach
    void setUp() {
        parsingService = new DocumentParsingServiceImpl(10_000_000);
    }

    @Test
    @DisplayName("Should successfully parse Plain Text (.txt) file")
    void testParsePlainText() {
        String content = "DocPulse AI is a multi-tenant enterprise RAG engine.";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        ParsedDocument parsed = parsingService.parse(bytes, "architecture.txt");

        assertThat(parsed).isNotNull();
        assertThat(parsed.getDocumentName()).isEqualTo("architecture.txt");
        assertThat(parsed.getRawText()).isEqualTo(content);
        assertThat(parsed.getDetectedMimeType()).contains("text/plain");
        assertThat(parsed.getWordCount()).isEqualTo(8);
        assertThat(parsed.hasContent()).isTrue();
    }

    @Test
    @DisplayName("Should successfully parse Markdown (.md) file")
    void testParseMarkdown() {
        String content = "# System Architecture\n\nDocPulse utilizes Java 21 Loom and Spring AI.";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        ParsedDocument parsed = parsingService.parse(bytes, "README.md");

        assertThat(parsed).isNotNull();
        assertThat(parsed.getDocumentName()).isEqualTo("README.md");
        assertThat(parsed.getRawText()).contains("System Architecture").contains("Spring AI");
        assertThat(parsed.hasContent()).isTrue();
    }

    @Test
    @DisplayName("Should successfully parse CSV (.csv) file")
    void testParseCsv() {
        String content = "tenant_id,department,document_name\ntenant-eng,Engineering,roadmap.pdf";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        ParsedDocument parsed = parsingService.parse(bytes, "tenants.csv");

        assertThat(parsed).isNotNull();
        assertThat(parsed.getRawText()).contains("tenant-eng").contains("Engineering");
    }

    @Test
    @DisplayName("Should successfully parse generated PDF document and extract metadata")
    void testParseValidPdf() throws IOException {
        byte[] pdfBytes = createSamplePdf("Confidential Employee Onboarding Guidelines");

        ParsedDocument parsed = parsingService.parse(pdfBytes, "onboarding.pdf");

        assertThat(parsed).isNotNull();
        assertThat(parsed.getDocumentName()).isEqualTo("onboarding.pdf");
        assertThat(parsed.getRawText()).contains("Confidential Employee Onboarding Guidelines");
        assertThat(parsed.getDetectedMimeType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Edge Case: Should throw EncryptedDocumentException when PDF is password-protected")
    void testParsePasswordProtectedPdfThrows() throws IOException {
        byte[] encryptedPdf = createEncryptedPdf("Secret Payroll Information", "SecretPassword123");

        assertThatThrownBy(() -> parsingService.parse(encryptedPdf, "payroll_locked.pdf"))
                .isInstanceOf(EncryptedDocumentException.class)
                .hasMessageContaining("password-protected or encrypted");
    }

    @Test
    @DisplayName("Edge Case: Should throw EmptyDocumentException when file is 0 bytes")
    void testParseZeroByteFileThrows() {
        byte[] emptyBytes = new byte[0];

        assertThatThrownBy(() -> parsingService.parse(emptyBytes, "empty.txt"))
                .isInstanceOf(EmptyDocumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("Edge Case: Should throw EmptyDocumentException when file contains only whitespace")
    void testParseWhitespaceOnlyFileThrows() {
        byte[] whitespaceBytes = "   \n\t  \r\n   ".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> parsingService.parse(whitespaceBytes, "blank.txt"))
                .isInstanceOf(EmptyDocumentException.class)
                .hasMessageContaining("no readable text or only whitespace");
    }

    @Test
    @DisplayName("Edge Case: Should throw UnsupportedDocumentTypeException when file type is outside whitelist")
    void testParseUnsupportedFileTypeThrows() {
        // Zip archive header bytes
        byte[] zipBytes = new byte[] { 0x50, 0x4B, 0x03, 0x04, 0x00, 0x00 };

        assertThatThrownBy(() -> parsingService.parse(zipBytes, "backup.zip"))
                .isInstanceOf(UnsupportedDocumentTypeException.class)
                .hasMessageContaining("application/zip");
    }

    @Test
    @DisplayName("Should successfully parse Spring MockMultipartFile")
    void testParseMultipartFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "policy.txt",
                "text/plain",
                "Company travel reimbursement policy.".getBytes(StandardCharsets.UTF_8)
        );

        ParsedDocument parsed = parsingService.parse(file);

        assertThat(parsed).isNotNull();
        assertThat(parsed.getDocumentName()).isEqualTo("policy.txt");
        assertThat(parsed.getRawText()).isEqualTo("Company travel reimbursement policy.");
    }

    @Test
    @DisplayName("Should ensure no Postgres null characters (\u0000) exist in parsed output")
    void testSanitizesNullCharacters() {
        String dirtyContent = "DocPulse\u0000 AI\u0000 Secure Ingestion\u0000";
        byte[] bytes = dirtyContent.getBytes(StandardCharsets.UTF_8);

        ParsedDocument parsed = parsingService.parse(bytes, "dirty.txt");

        assertThat(parsed.getRawText()).doesNotContain("\u0000");
        assertThat(parsed.getRawText()).contains("DocPulse").contains("Secure Ingestion");
    }

    private byte[] createSamplePdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] createEncryptedPdf(String text, String password) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            AccessPermission permissions = new AccessPermission();
            StandardProtectionPolicy policy = new StandardProtectionPolicy(password, password, permissions);
            policy.setEncryptionKeyLength(128);
            document.protect(policy);

            document.save(out);
            return out.toByteArray();
        }
    }
}
