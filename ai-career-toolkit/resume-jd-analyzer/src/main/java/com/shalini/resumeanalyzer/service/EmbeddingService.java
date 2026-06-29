package com.shalini.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shalini.resumeanalyzer.model.Chunk;
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

    public EmbeddingService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Generates an embedding vector for a single text string.
     * Returns a List<Double> of 1536 dimensions (for ada-002).
     */
    public List<Double> getEmbedding(String text) {
        Map<String, Object> requestBody = Map.of(
            "model", embeddingModel,
            "input", text
        );

        String response = webClient.post()
            .uri(embeddingUrl)
            .header("Authorization", "Bearer " + openAiApiKey)
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        return parseEmbedding(response);
    }

    /**
     * Generates embeddings for a list of text chunks.
     * Returns a list of Chunk objects, each with their embedding vector set.
     */
    public List<Chunk> embedChunks(List<String> texts, String source) {
        List<Chunk> chunks = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            List<Double> embedding = getEmbedding(text);
            chunks.add(new Chunk(text, embedding, source, i));
        }
        return chunks;
    }

    /**
     * Computes cosine similarity between two embedding vectors.
     * Returns a value between -1 and 1 (1 = identical, 0 = unrelated).
     *
     * Formula: cos(θ) = (A · B) / (|A| × |B|)
     */
    public double cosineSimilarity(List<Double> vecA, List<Double> vecB) {
        if (vecA.size() != vecB.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        double dotProduct = 0.0;
        double magnitudeA = 0.0;
        double magnitudeB = 0.0;

        for (int i = 0; i < vecA.size(); i++) {
            dotProduct  += vecA.get(i) * vecB.get(i);
            magnitudeA  += vecA.get(i) * vecA.get(i);
            magnitudeB  += vecB.get(i) * vecB.get(i);
        }

        double denominator = Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB);
        if (denominator == 0) return 0.0;

        return dotProduct / denominator;
    }

    /**
     * Given a query embedding (resume embedding), retrieves the top-k most
     * similar chunks from the JD chunk list using cosine similarity.
     */
    public List<Chunk> retrieveTopK(List<Double> queryEmbedding, List<Chunk> jdChunks, int topK) {
        // Score each JD chunk by similarity to the query
        List<Map.Entry<Chunk, Double>> scored = new ArrayList<>();
        for (Chunk chunk : jdChunks) {
            double score = cosineSimilarity(queryEmbedding, chunk.getEmbedding());
            scored.add(Map.entry(chunk, score));
        }

        // Sort by descending similarity score
        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // Return top-k chunks
        List<Chunk> topChunks = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            topChunks.add(scored.get(i).getKey());
        }
        return topChunks;
    }

    /**
     * Parses the OpenAI embedding API JSON response to extract the vector.
     */
    private List<Double> parseEmbedding(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode embeddingArray = root.path("data").get(0).path("embedding");

            List<Double> embedding = new ArrayList<>();
            for (JsonNode val : embeddingArray) {
                embedding.add(val.asDouble());
            }
            return embedding;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse embedding response: " + e.getMessage(), e);
        }
    }
}
