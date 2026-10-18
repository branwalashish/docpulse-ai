package com.docpulse.ai.ingestion.service;

import com.docpulse.ai.common.exception.DocumentParsingException;
import com.docpulse.ai.common.exception.EmptyDocumentException;
import com.docpulse.ai.common.exception.EncryptedDocumentException;
import com.docpulse.ai.common.exception.UnsupportedDocumentTypeException;
import com.docpulse.ai.ingestion.model.ParsedDocument;
import com.docpulse.ai.ingestion.model.SupportedContentType;
import com.docpulse.ai.ingestion.util.TextSanitizer;
import org.apache.tika.Tika;
import org.apache.tika.exception.ZeroByteFileException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of DocumentParsingService using Apache Tika 2.9.2.
 * Defensively extracts text and metadata with MIME detection, character limit guards,
 * and edge-case protection against password-protected, corrupt, or empty documents.
 */
@Service
public class DocumentParsingServiceImpl implements DocumentParsingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentParsingServiceImpl.class);

    private final Tika tika;
    private final AutoDetectParser parser;
    private final int maxCharacterLimit;

    public DocumentParsingServiceImpl(
            @Value("${docpulse.parsing.max-character-limit:10000000}") int maxCharacterLimit) {
        this.tika = new Tika();
        this.parser = new AutoDetectParser();
        this.maxCharacterLimit = maxCharacterLimit > 0 ? maxCharacterLimit : 10_000_000;
        log.info("Initialized DocumentParsingServiceImpl with max character limit: {}", this.maxCharacterLimit);
    }

    @Override
    public ParsedDocument parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            String name = file != null ? file.getOriginalFilename() : "unknown";
            throw new EmptyDocumentException(name, "Uploaded file is null or empty (0 bytes)");
        }

        try (InputStream is = file.getInputStream()) {
            return parse(is, file.getOriginalFilename());
        } catch (IOException e) {
            throw new DocumentParsingException(file.getOriginalFilename(), "Failed to read uploaded file stream: " + e.getMessage(), e);
        }
    }

    @Override
    public ParsedDocument parse(byte[] fileBytes, String filename) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new EmptyDocumentException(filename, "Document byte array is null or empty");
        }
        return parse(new ByteArrayInputStream(fileBytes), filename);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) {
        String safeFilename = (filename != null && !filename.isBlank()) ? filename : "unnamed_document";

        if (inputStream == null) {
            throw new EmptyDocumentException(safeFilename, "Document input stream cannot be null");
        }

        try (TikaInputStream tikaStream = TikaInputStream.get(inputStream)) {
            // 1. Detect MIME type defensively from magic bytes and filename
            Metadata detectMetadata = new Metadata();
            detectMetadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, safeFilename);

            String detectedMimeType;
            try {
                detectedMimeType = tika.detect(tikaStream, detectMetadata);
            } catch (IOException e) {
                throw new DocumentParsingException(safeFilename, "Failed to detect MIME type: " + e.getMessage(), e);
            }

            log.debug("Document '{}' detected as MIME type '{}'", safeFilename, detectedMimeType);

            // 2. Validate against whitelisted supported MIME types
            if (!SupportedContentType.isSupported(detectedMimeType)) {
                log.warn("Document '{}' has unsupported MIME type '{}'", safeFilename, detectedMimeType);
                throw new UnsupportedDocumentTypeException(safeFilename, detectedMimeType);
            }

            // 3. Configure Tika parsing with character limit guard
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, safeFilename);
            metadata.set(Metadata.CONTENT_TYPE, detectedMimeType);

            BodyContentHandler handler = new BodyContentHandler(this.maxCharacterLimit);
            ParseContext context = new ParseContext();
            context.set(Parser.class, this.parser);

            // 4. Execute parsing with edge-case handling
            try {
                parser.parse(tikaStream, handler, metadata, context);
            } catch (ZeroByteFileException e) {
                log.warn("Document '{}' is a zero-byte file", safeFilename);
                throw new EmptyDocumentException(safeFilename, "File is zero bytes or has no content");
            } catch (org.apache.tika.exception.EncryptedDocumentException e) {
                log.warn("Document '{}' is password-protected or encrypted", safeFilename);
                throw new EncryptedDocumentException(safeFilename, e);
            } catch (Exception e) {
                if (isEncryptionRelated(e)) {
                    log.warn("Document '{}' failed with encryption error: {}", safeFilename, e.getMessage());
                    throw new EncryptedDocumentException(safeFilename, e);
                }
                log.error("Parsing failed for document '{}': {}", safeFilename, e.getMessage());
                throw new DocumentParsingException(safeFilename, "Extraction failed: " + e.getMessage(), e);
            }

            // 5. Sanitize text and remove Postgres-incompatible null characters
            String rawText = handler.toString();
            String sanitizedText = TextSanitizer.sanitize(rawText);

            if (sanitizedText.isEmpty()) {
                log.warn("Document '{}' yielded no readable text after sanitization", safeFilename);
                throw new EmptyDocumentException(safeFilename, "Document contains no readable text or only whitespace");
            }

            // 6. Extract document properties from Tika Metadata
            Map<String, Object> documentProperties = extractProperties(metadata);

            log.info("Successfully parsed document '{}' ({} characters, {} words, MIME: {})",
                    safeFilename, sanitizedText.length(), calculateWordCount(sanitizedText), detectedMimeType);

            return ParsedDocument.builder()
                    .documentId(UUID.randomUUID().toString())
                    .documentName(safeFilename)
                    .rawText(sanitizedText)
                    .detectedMimeType(detectedMimeType)
                    .documentProperties(documentProperties)
                    .build();

        } catch (IOException e) {
            throw new DocumentParsingException(safeFilename, "I/O failure while processing document stream: " + e.getMessage(), e);
        }
    }

    private boolean isEncryptionRelated(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            String msg = current.getMessage() != null ? current.getMessage().toLowerCase() : "";
            if (className.contains("Encrypted") || className.contains("Password")
                    || msg.contains("password") || msg.contains("encrypted") || msg.contains("bad password")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Map<String, Object> extractProperties(Metadata metadata) {
        Map<String, Object> properties = new HashMap<>();
        for (String name : metadata.names()) {
            String value = metadata.get(name);
            if (value != null && !value.isBlank()) {
                properties.put(name, value);
            }
        }
        return properties;
    }

    private int calculateWordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}

