package com.shalini.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shalini.resumeanalyzer.model.Chunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentService {

    @Value("${claude.api.key}")
    private String claudeApiKey;

    @Value("${claude.api.url}")
    private String claudeApiUrl;

    @Value("${claude.model}")
    private String claudeModel;

    @Value("${claude.max-tokens}")
    private int maxTokens;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public AgentService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * MAIN AGENT METHOD
     *
     * This is the core of the RAG + AI Agent pipeline.
     * It takes the resume text and the retrieved top JD chunks,
     * builds a detailed prompt, calls Claude, and returns structured JSON.
     *
     * The Claude API call follows Anthropic's /v1/messages format.
     */
    public Map<String, Object> analyzeMatch(String resumeText, List<Chunk> topJdChunks) {

        // Step 1: Build context string from retrieved JD chunks
        String jdContext = topJdChunks.stream()
            .map(Chunk::getText)
            .collect(Collectors.joining("\n\n---\n\n"));

        // Step 2: Build the user message (Resume + Retrieved JD context)
        String userMessage = """
            ## Candidate Resume
            %s

            ## Most Relevant Job Description Sections (retrieved by semantic search)
            %s
            """.formatted(resumeText, jdContext);

        // Step 3: Build the Claude API request body
        Map<String, Object> requestBody = Map.of(
            "model", claudeModel,
            "max_tokens", maxTokens,
            "system", buildSystemPrompt(),
            "messages", List.of(
                Map.of("role", "user", "content", userMessage)
            )
        );

        // Step 4: Call Claude API
        String response = webClient.post()
            .uri(claudeApiUrl)
            .header("x-api-key", claudeApiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        // Step 5: Parse and return the structured result
        return parseAgentResponse(response);
    }

    /**
     * System prompt that tells Claude exactly what to do and how to respond.
     *
     * Key principles:
     * - Clear role definition
     * - Strict JSON output format (so we can parse it in Java)
     * - Specific fields we need for the UI
     */
    private String buildSystemPrompt() {
        return """
            You are an expert technical recruiter and resume coach with 10+ years of experience
            in software engineering hiring, especially for Java, Spring Boot, and full-stack roles.

            Your job is to analyze a candidate's resume against a job description and provide
            a detailed, honest, and actionable match analysis.

            You will receive:
            1. The candidate's full resume text
            2. The most semantically relevant sections from the job description

            You MUST respond with ONLY a valid JSON object (no markdown, no explanation outside JSON).
            Use exactly this structure:

            {
              "matchScore": <integer 0-100>,
              "matchLevel": "<one of: Strong Match, Partial Match, Weak Match>",
              "jobTitle": "<extracted job title from JD>",
              "matchedSkills": [
                "<skill1>", "<skill2>", "<skill3>"
              ],
              "missingSkills": [
                "<missing skill 1>", "<missing skill 2>"
              ],
              "rewriteSuggestions": [
                {
                  "original": "<original resume bullet or section>",
                  "suggested": "<improved version tailored to the JD>",
                  "reason": "<why this change helps>"
                }
              ],
              "overallSummary": "<2-3 sentence honest summary of the match and top recommendation>"
            }

            Scoring guide:
            - 75-100: Strong Match — candidate meets most requirements
            - 50-74: Partial Match — candidate has core skills but gaps exist
            - 0-49: Weak Match — significant skill gaps

            Be specific and honest. Don't inflate scores. Focus on technical skills, not soft skills.
            """;
    }

    /**
     * Parses Claude's JSON response into a Java Map.
     * Claude returns: { "content": [{ "type": "text", "text": "..." }] }
     * We extract the inner text and parse it as our analysis JSON.
     */
    private Map<String, Object> parseAgentResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);

            // Extract the text content from Claude's response
            String text = root
                .path("content")
                .get(0)
                .path("text")
                .asText();

            // Strip any accidental markdown fences
            text = text.replaceAll("```json", "").replaceAll("```", "").trim();

            // Parse as a Map
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(text, Map.class);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Claude response: " + e.getMessage(), e);
        }
    }
}
