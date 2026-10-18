package com.docpulse.ai.ingestion.model;

import com.docpulse.ai.ingestion.dto.ParsedDocumentDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParsedDocumentTest {

    @Test
    @DisplayName("Should successfully construct ParsedDocument and calculate word/character count")
    void testParsedDocumentCreation() {
        String text = "Enterprise RAG system with Spring AI and pgvector.";
        ParsedDocument doc = ParsedDocument.builder()
                .documentName("test.pdf")
                .rawText(text)
                .detectedMimeType("application/pdf")
                .addProperty("pdf:PageCount", 3)
                .build();

        assertThat(doc.getDocumentId()).isNotNull().isNotBlank();
        assertThat(doc.getDocumentName()).isEqualTo("test.pdf");
        assertThat(doc.getRawText()).isEqualTo(text);
        assertThat(doc.getDetectedMimeType()).isEqualTo("application/pdf");
        assertThat(doc.getCharacterCount()).isEqualTo(text.length());
        assertThat(doc.getWordCount()).isEqualTo(8);
        assertThat(doc.getDocumentProperties()).containsEntry("pdf:PageCount", 3);
        assertThat(doc.hasContent()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when documentName is null")
    void testDocumentNameNullThrows() {
        assertThatThrownBy(() -> ParsedDocument.builder()
                .documentName(null)
                .rawText("Some text")
                .build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("documentName cannot be null");
    }

    @Test
    @DisplayName("Should handle empty rawText gracefully")
    void testEmptyRawText() {
        ParsedDocument doc = ParsedDocument.builder()
                .documentName("empty.txt")
                .rawText("")
                .build();

        assertThat(doc.hasContent()).isFalse();
        assertThat(doc.getCharacterCount()).isZero();
        assertThat(doc.getWordCount()).isZero();
    }

    @Test
    @DisplayName("Should convert ParsedDocument to ParsedDocumentDto and back")
    void testDtoConversion() {
        ParsedDocument domain = ParsedDocument.builder()
                .documentName("handbook.docx")
                .rawText("Welcome to the engineering handbook.")
                .detectedMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .documentProperties(Map.of("author", "HR Team"))
                .build();

        ParsedDocumentDto dto = ParsedDocumentDto.fromDomain(domain);

        assertThat(dto).isNotNull();
        assertThat(dto.getDocumentId()).isEqualTo(domain.getDocumentId());
        assertThat(dto.getDocumentName()).isEqualTo("handbook.docx");
        assertThat(dto.getWordCount()).isEqualTo(5);
        assertThat(dto.getDocumentProperties()).containsEntry("author", "HR Team");
    }
}
