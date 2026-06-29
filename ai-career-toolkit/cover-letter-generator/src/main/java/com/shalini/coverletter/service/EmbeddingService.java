package com.shalini.coverletter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shalini.coverletter.model.Chunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;

@Service
public class EmbeddingService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.embedding.url}")
    private String embeddingUrl;

    @Value("${openai.embedding.model}")
    private String embeddingModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public EmbeddingService(WebClient.Builder builder, ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.objectMapper = objectMapper;
    }

    public List<Double> getEmbedding(String text) {
        Map<String, Object> body = Map.of("model", embeddingModel, "input", text);
        String response = webClient.post()
            .uri(embeddingUrl)
            .header("Authorization", "Bearer " + openAiApiKey)
            .header("Content-Type", "application/json")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .block();
        return parseEmbedding(response);
    }

    public List<Chunk> embedChunks(List<String> texts, String source) {
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            chunks.add(new Chunk(texts.get(i), getEmbedding(texts.get(i)), source, i));
        }
        return chunks;
    }

    public double cosineSimilarity(List<Double> a, List<Double> b) {
        double dot = 0, magA = 0, magB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot  += a.get(i) * b.get(i);
            magA += a.get(i) * a.get(i);
            magB += b.get(i) * b.get(i);
        }
        double denom = Math.sqrt(magA) * Math.sqrt(magB);
        return denom == 0 ? 0 : dot / denom;
    }

    public List<Chunk> retrieveTopK(List<Double> queryEmbedding, List<Chunk> chunks, int k) {
        List<Map.Entry<Chunk, Double>> scored = new ArrayList<>();
        for (Chunk c : chunks) {
            scored.add(Map.entry(c, cosineSimilarity(queryEmbedding, c.getEmbedding())));
        }
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Chunk> top = new ArrayList<>();
        for (int i = 0; i < Math.min(k, scored.size()); i++) top.add(scored.get(i).getKey());
        return top;
    }

    private List<Double> parseEmbedding(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode arr = root.path("data").get(0).path("embedding");
            List<Double> vec = new ArrayList<>();
            for (JsonNode v : arr) vec.add(v.asDouble());
            return vec;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse embedding: " + e.getMessage(), e);
        }
    }
}
