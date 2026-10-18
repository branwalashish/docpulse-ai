package com.docpulse.ai.ingestion.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SupportedContentTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "application/pdf",
            "application/pdf; charset=UTF-8",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "text/plain",
            "text/markdown",
            "text/csv"
    })
    @DisplayName("Should recognize all supported MIME types")
    void testSupportedMimeTypes(String mimeType) {
        assertThat(SupportedContentType.isSupported(mimeType)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "application/zip",
            "application/x-executable",
            "video/mp4",
            "image/png",
            "application/json",
            "",
            "   "
    })
    @DisplayName("Should reject unsupported or blank MIME types")
    void testUnsupportedMimeTypes(String mimeType) {
        assertThat(SupportedContentType.isSupported(mimeType)).isFalse();
    }

    @Test
    @DisplayName("Should resolve SupportedContentType by filename extension")
    void testFromFilename() {
        assertThat(SupportedContentType.fromFilename("handbook.pdf")).contains(SupportedContentType.PDF);
        assertThat(SupportedContentType.fromFilename("contract.DOCX")).contains(SupportedContentType.DOCX);
        assertThat(SupportedContentType.fromFilename("notes.txt")).contains(SupportedContentType.TEXT_PLAIN);
        assertThat(SupportedContentType.fromFilename("readme.md")).contains(SupportedContentType.MARKDOWN);
        assertThat(SupportedContentType.fromFilename("data.csv")).contains(SupportedContentType.CSV);
        assertThat(SupportedContentType.fromFilename("archive.tar.gz")).isEmpty();
        assertThat(SupportedContentType.fromFilename("no_extension")).isEmpty();
    }

    @Test
    @DisplayName("Should resolve SupportedContentType from canonical MIME type")
    void testFromMimeType() {
        Optional<SupportedContentType> type = SupportedContentType.fromMimeType("application/pdf");
        assertThat(type).isPresent().contains(SupportedContentType.PDF);
    }
}
