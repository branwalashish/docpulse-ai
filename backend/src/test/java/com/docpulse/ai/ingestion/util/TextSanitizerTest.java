package com.docpulse.ai.ingestion.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextSanitizerTest {

    @Test
    @DisplayName("Should return empty string on null or empty input")
    void testNullAndEmpty() {
        assertThat(TextSanitizer.sanitize(null)).isEmpty();
        assertThat(TextSanitizer.sanitize("")).isEmpty();
        assertThat(TextSanitizer.sanitize("    ")).isEmpty();
    }

    @Test
    @DisplayName("Should strip PostgreSQL-incompatible null characters (\u0000)")
    void testNullByteRemoval() {
        String inputWithNullBytes = "DocPulse\u0000 AI\u0000 RAG Engine\u0000";
        String sanitized = TextSanitizer.sanitize(inputWithNullBytes);

        assertThat(sanitized).isEqualTo("DocPulse AI RAG Engine");
        assertThat(sanitized).doesNotContain("\u0000");
    }

    @Test
    @DisplayName("Should strip illegal ASCII control characters while keeping valid formatting")
    void testControlCharRemoval() {
        // \u0007 is bell, \u0001 is start of heading, \u001B is escape
        String input = "Section 1\u0001\u0007 Overview\u001B:\nContent details.";
        String sanitized = TextSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("Section 1 Overview:\nContent details.");
    }

    @Test
    @DisplayName("Should normalize CRLF and collapse excessive newlines to paragraph breaks (\n\n)")
    void testNewlineNormalization() {
        String inputWithCrLfAndExcessiveLines = "Header\r\n\r\n\r\n\r\n\r\nParagraph 1.\r\r\rParagraph 2.";
        String sanitized = TextSanitizer.sanitize(inputWithCrLfAndExcessiveLines);

        assertThat(sanitized).isEqualTo("Header\n\nParagraph 1.\n\nParagraph 2.");
    }

    @Test
    @DisplayName("Should collapse consecutive horizontal whitespace and spaces around newlines")
    void testHorizontalWhitespaceCollapsing() {
        String input = "  This    sentence    has    too   many   spaces.  \n   Next line.   ";
        String sanitized = TextSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("This sentence has too many spaces.\nNext line.");
    }

    @Test
    @DisplayName("Should replace non-breaking spaces (\u00A0) with standard spaces")
    void testNonBreakingSpace() {
        String input = "Non\u00A0breaking\u00A0space.";
        String sanitized = TextSanitizer.sanitize(input);

        assertThat(sanitized).isEqualTo("Non breaking space.");
    }

    @Test
    @DisplayName("isBlank should accurately identify empty, whitespace-only, or control-char-only strings")
    void testIsBlank() {
        assertThat(TextSanitizer.isBlank(null)).isTrue();
        assertThat(TextSanitizer.isBlank("")).isTrue();
        assertThat(TextSanitizer.isBlank("   \n\t  ")).isTrue();
        assertThat(TextSanitizer.isBlank("\u0000\u0000\u0007")).isTrue();
        assertThat(TextSanitizer.isBlank("Valid content")).isFalse();
    }
}
