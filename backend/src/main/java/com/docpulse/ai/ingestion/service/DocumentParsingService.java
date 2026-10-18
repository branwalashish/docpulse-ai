package com.docpulse.ai.ingestion.service;

import com.docpulse.ai.common.exception.DocumentParsingException;
import com.docpulse.ai.common.exception.EmptyDocumentException;
import com.docpulse.ai.common.exception.EncryptedDocumentException;
import com.docpulse.ai.common.exception.UnsupportedDocumentTypeException;
import com.docpulse.ai.ingestion.model.ParsedDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Service contract for parsing uploaded enterprise documents (PDF, DOCX, TXT, MD, CSV)
 * into structured text and metadata.
 */
public interface DocumentParsingService {

    /**
     * Parses a document from an InputStream.
     *
     * @param inputStream InputStream of the document.
     * @param filename    Original filename for format detection and metadata attribution.
     * @return ParsedDocument containing sanitized text and extracted properties.
     * @throws EncryptedDocumentException     if the document is password-protected.
     * @throws EmptyDocumentException         if the document contains 0 bytes or only whitespace.
     * @throws UnsupportedDocumentTypeException if the detected MIME type is outside the whitelist.
     * @throws DocumentParsingException       on corrupt streams or general extraction errors.
     */
    ParsedDocument parse(InputStream inputStream, String filename);

    /**
     * Parses a document from a raw byte array.
     *
     * @param fileBytes Raw byte content of the document.
     * @param filename  Original filename.
     * @return ParsedDocument containing sanitized text and extracted properties.
     */
    ParsedDocument parse(byte[] fileBytes, String filename);

    /**
     * Parses an uploaded Spring MultipartFile.
     *
     * @param file Uploaded multipart document.
     * @return ParsedDocument containing sanitized text and extracted properties.
     */
    ParsedDocument parse(MultipartFile file);
}

