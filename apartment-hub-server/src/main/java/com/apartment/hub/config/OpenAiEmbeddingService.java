package com.apartment.hub.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom EmbeddingModel that calls OpenAI's embedding API directly.
 * Separate from the chat model (which uses DeepSeek).
 */
@Slf4j
@Component
public class OpenAiEmbeddingService implements EmbeddingModel {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    private static final String EMBEDDING_URL = "https://api.openai.com/v1/embeddings";
    private static final int DIMENSIONS = 1536;  // text-embedding-3-small

    public OpenAiEmbeddingService(ObjectMapper objectMapper,
                                   @Value("${openai.embedding.api-key:${OPENAI_API_KEY:}}") String apiKey,
                                   @Value("${openai.embedding.model:text-embedding-3-small}") String model) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder().build();

        if (apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY not set — embedding/RAG features will not work");
        } else {
            log.info("OpenAI Embedding service initialized: model={}, dimensions={}", model, DIMENSIONS);
        }
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getContent());
    }

    @Override
    public float[] embed(String text) {
        List<float[]> results = embed(List.of(text));
        return results.isEmpty() ? new float[0] : results.get(0);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (apiKey.isBlank()) {
            log.error("Cannot embed: OPENAI_API_KEY not set");
            return List.of(new float[DIMENSIONS]);
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "input", texts
            );

            String response = restClient.post()
                    .uri(EMBEDDING_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.get("data");
            List<float[]> embeddings = new ArrayList<>();
            for (JsonNode item : data) {
                JsonNode embedding = item.get("embedding");
                float[] vec = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    vec[i] = (float) embedding.get(i).asDouble();
                }
                embeddings.add(vec);
            }
            log.debug("Embedded {} texts → {} vectors", texts.size(), embeddings.size());
            return embeddings;

        } catch (Exception e) {
            log.error("OpenAI embedding failed", e);
            return texts.stream().map(t -> new float[DIMENSIONS]).toList();
        }
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<float[]> embeddings = embed(request.getInstructions());
        return new EmbeddingResponse(embeddings.stream()
                .map(e -> new org.springframework.ai.embedding.Embedding(e, 0))
                .toList());
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }
}
