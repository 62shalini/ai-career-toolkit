package com.shalini.coverletter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shalini.coverletter.model.Chunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CoverLetterAgentService {

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

    public CoverLetterAgentService(WebClient.Builder builder, ObjectMapper objectMapper) {
        this.webClient = builder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * CORE AGENT METHOD
     *
     * What makes this RAG-powered and not just a simple prompt:
     * - The JD is chunked and embedded
     * - Top-K chunks semantically closest to the resume are retrieved
     * - Only those chunks go into the prompt (not the entire JD)
     * - This means the agent focuses on exactly what matches the candidate's background
     * - The cover letter mirrors the JD's language naturally
     */
    public String generateCoverLetter(
            String resumeText,
            List<Chunk> topJdChunks,
            String candidateName,
            String candidateEmail,
            String candidatePhone,
            String companyName,
            String jobTitle,
            String hiringManagerName,
            String tone) {

        // Build context from retrieved JD chunks
        String jdContext = topJdChunks.stream()
            .map(Chunk::getText)
            .collect(Collectors.joining("\n\n---\n\n"));

        // Greeting line: use manager name if provided, else generic
        String greeting = (hiringManagerName != null && !hiringManagerName.isBlank())
            ? "Dear " + hiringManagerName + ","
            : "Dear Hiring Manager,";

        String userMessage = """
            ## Candidate Details
            Name: %s
            Email: %s
            Phone: %s

            ## Target Role
            Company: %s
            Job Title: %s
            Tone requested: %s
            Greeting to use: %s

            ## Candidate's Resume
            %s

            ## Most Relevant Job Description Sections (retrieved by semantic search)
            %s
            """.formatted(
                candidateName, candidateEmail, candidatePhone,
                companyName, jobTitle, tone, greeting,
                resumeText, jdContext
            );

        Map<String, Object> requestBody = Map.of(
            "model", claudeModel,
            "max_tokens", maxTokens,
            "system", buildSystemPrompt(tone),
            "messages", List.of(Map.of("role", "user", "content", userMessage))
        );

        String response = webClient.post()
            .uri(claudeApiUrl)
            .header("x-api-key", claudeApiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        return parseResponse(response);
    }

    /**
     * System prompt — the key difference from the Resume Analyzer.
     *
     * Notice: we tell Claude to mirror the JD's language. This is RAG's magic —
     * because the JD chunks are already in the prompt, Claude naturally uses
     * the same keywords, phrases, and terminology the JD uses. This makes the
     * cover letter feel highly tailored and pass ATS keyword scanners.
     */
    private String buildSystemPrompt(String tone) {
        String toneInstruction = switch (tone) {
            case "Enthusiastic" -> "Write with genuine enthusiasm and energy. Show passion for the role.";
            case "Concise"      -> "Be direct and brief. 3 short paragraphs max. No filler sentences.";
            default             -> "Write professionally and confidently. Formal but warm.";
        };

        return """
            You are an expert cover letter writer who specializes in software engineering and tech roles.
            You write highly personalized, ATS-optimized cover letters that feel human and authentic.

            Tone instruction: %s

            Rules you MUST follow:
            1. Start with the greeting provided (e.g. "Dear Hiring Manager,")
            2. Opening paragraph: hook with ONE specific, impressive achievement from the resume
            3. Middle paragraph(s): connect candidate's skills directly to the JD requirements using
               the EXACT keywords and phrases from the JD context provided — this is critical for ATS
            4. Closing paragraph: express genuine interest in the company, call to action
            5. End with: "Sincerely," followed by the candidate's name on the next line
            6. Do NOT use clichés: "I am writing to apply", "I am a quick learner", "team player"
            7. Do NOT make up achievements not present in the resume
            8. Length: 300-400 words (unless "Concise" tone, then 150-200 words)
            9. Output ONLY the cover letter text — no intro, no explanation, no markdown

            The cover letter must feel like it was written specifically for this company and role,
            not a generic template with company name swapped in.
            """.formatted(toneInstruction);
    }

    private String parseResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            return root.path("content").get(0).path("text").asText().trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Claude response: " + e.getMessage(), e);
        }
    }
}
