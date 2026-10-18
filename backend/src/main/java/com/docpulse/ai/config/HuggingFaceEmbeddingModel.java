package com.docpulse.ai.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Custom EmbeddingModel for DocPulse AI.
 * Communicates with the HuggingFace Inference API for 'BAAI/bge-small-en-v1.5' (384 dimensions).
 * Includes an automated deterministic fallback mechanism for offline testing, local development,
 * or when the HuggingFace API is rate-limited / key is omitted.
 */
public class HuggingFaceEmbeddingModel extends AbstractEmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceEmbeddingModel.class);

    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final int dimensions;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HuggingFaceEmbeddingModel(String baseUrl, String model, String apiKey, int dimensions, RestClient.Builder restClientBuilder) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.model = model;
        this.apiKey = apiKey;
        this.dimensions = dimensions > 0 ? dimensions : 384;
        this.objectMapper = new ObjectMapper();

        RestClient.Builder builder = restClientBuilder != null ? restClientBuilder : RestClient.builder();
        this.restClient = builder
                .baseUrl(this.baseUrl + this.model)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("Initialized HuggingFaceEmbeddingModel with model: '{}', target dimensions: {}", this.model, this.dimensions);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getFormattedContent(MetadataMode.NONE));
    }

    @Override
    public float[] embed(String text) {
        List<float[]> embeddings = embed(List.of(text));
        return embeddings.isEmpty() ? generateDeterministicFallbackVector(text) : embeddings.get(0);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        // If no valid API key is configured, use deterministic local embedding generator
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your_")) {
            log.debug("HuggingFace API key not provided; using deterministic local 384-dim embeddings");
            return texts.stream().map(this::generateDeterministicFallbackVector).toList();
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("inputs", texts);
            payload.put("options", Map.of("wait_for_model", true));

            String responseBody = restClient.post()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("Empty response received from HuggingFace API; falling back to local embeddings");
                return texts.stream().map(this::generateDeterministicFallbackVector).toList();
            }

            // Parse response: can be 2D array [[...], [...]] or single 1D array [...]
            if (responseBody.trim().startsWith("[[")) {
                List<List<Double>> list = objectMapper.readValue(responseBody, new TypeReference<List<List<Double>>>() {});
                List<float[]> result = new ArrayList<>();
                for (List<Double> vector : list) {
                    result.add(toDoubleToFloatArray(vector));
                }
                return result;
            } else if (responseBody.trim().startsWith("[")) {
                List<Double> vector = objectMapper.readValue(responseBody, new TypeReference<List<Double>>() {});
                return List.of(toDoubleToFloatArray(vector));
            } else {
                log.warn("Unexpected HuggingFace API response format: {}. Using fallback.", responseBody);
                return texts.stream().map(this::generateDeterministicFallbackVector).toList();
            }
        } catch (Exception ex) {
            log.warn("HuggingFace Inference API call failed ({}). Utilizing fallback embeddings.", ex.getMessage());
            return texts.stream().map(this::generateDeterministicFallbackVector).toList();
        }
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> instructions = request.getInstructions();
        List<float[]> vectors = embed(instructions);
        List<Embedding> embeddings = new ArrayList<>();

        for (int i = 0; i < vectors.size(); i++) {
            embeddings.add(new Embedding(vectors.get(i), i));
        }

        return new EmbeddingResponse(embeddings, new EmbeddingResponseMetadata(this.model, null));
    }

    @Override
    public int dimensions() {
        return this.dimensions;
    }

    private float[] toDoubleToFloatArray(List<Double> vector) {
        float[] arr = new float[this.dimensions];
        int size = Math.min(vector.size(), this.dimensions);
        for (int i = 0; i < size; i++) {
            arr[i] = vector.get(i).floatValue();
        }
        return normalize(arr);
    }

    /**
     * Generates a deterministic unit-normalized pseudo-embedding vector of the specified dimensions.
     * Guaranteed to produce the exact same vector for identical text inputs.
     */
    public float[] generateDeterministicFallbackVector(String text) {
        float[] vector = new float[this.dimensions];
        long seed = hashStringToSeed(text != null ? text : "");
        Random random = new Random(seed);

        for (int i = 0; i < this.dimensions; i++) {
            vector[i] = (float) (random.nextGaussian());
        }

        return normalize(vector);
    }

    private float[] normalize(float[] v) {
        double sum = 0.0;
        for (float f : v) {
            sum += f * f;
        }
        double norm = Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) {
                v[i] = (float) (v[i] / norm);
            }
        }
        return v;
    }

    private long hashStringToSeed(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(str.getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hash[i] & 0xFF);
            }
            return seed;
        } catch (NoSuchAlgorithmException e) {
            return str.hashCode();
        }
    }
}

