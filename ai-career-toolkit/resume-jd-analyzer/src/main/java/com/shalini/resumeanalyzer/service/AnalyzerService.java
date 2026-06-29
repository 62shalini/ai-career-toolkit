package com.shalini.resumeanalyzer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shalini.resumeanalyzer.model.AnalysisResult;
import com.shalini.resumeanalyzer.model.Chunk;
import com.shalini.resumeanalyzer.repository.AnalysisResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyzerService {

    private final PdfParserService pdfParserService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final AgentService agentService;
    private final AnalysisResultRepository repository;
    private final ObjectMapper objectMapper;

    private static final int TOP_K = 5; // how many JD chunks to retrieve

    /**
     * FULL RAG + AGENT PIPELINE
     *
     * Step 1: Parse resume PDF → extract text
     * Step 2: Parse JD (PDF or plain text) → extract text
     * Step 3: Chunk the JD text
     * Step 4: Embed each JD chunk into a vector
     * Step 5: Embed the full resume as a query vector
     * Step 6: Retrieve top-K most similar JD chunks (semantic search)
     * Step 7: Pass resume + retrieved chunks to Claude Agent
     * Step 8: Parse the structured JSON response
     * Step 9: Save results to MySQL
     * Step 10: Return AnalysisResult for rendering
     */
    public AnalysisResult analyze(MultipartFile resumeFile, String jdText, MultipartFile jdFile)
            throws IOException {

        // Step 1 & 2: Extract text
        String resumeText = pdfParserService.extractText(resumeFile);
        String jobDescriptionText;

        if (jdFile != null && !jdFile.isEmpty()) {
            jobDescriptionText = pdfParserService.extractText(jdFile);
        } else {
            jobDescriptionText = pdfParserService.extractFromString(jdText);
        }

        // Step 3: Chunk the JD
        List<String> jdChunkTexts = chunkingService.chunk(jobDescriptionText);

        // Step 4: Embed JD chunks
        List<Chunk> jdChunks = embeddingService.embedChunks(jdChunkTexts, "jd");

        // Step 5: Embed the resume (treat full resume as one query)
        // We truncate to ~4000 chars to stay within embedding token limits
        String resumeQuery = resumeText.length() > 4000
            ? resumeText.substring(0, 4000)
            : resumeText;
        List<Double> resumeEmbedding = embeddingService.getEmbedding(resumeQuery);

        // Step 6: Retrieve top-K JD chunks most similar to resume
        List<Chunk> topJdChunks = embeddingService.retrieveTopK(resumeEmbedding, jdChunks, TOP_K);

        // Step 7 & 8: Call Claude Agent, get structured result
        Map<String, Object> agentResult = agentService.analyzeMatch(resumeText, topJdChunks);

        // Step 9: Build and save AnalysisResult entity
        AnalysisResult result = buildResult(agentResult, resumeFile.getOriginalFilename(),
                                             resumeText, jobDescriptionText);
        return repository.save(result);
    }

    /**
     * Maps the agent's JSON output into our JPA entity.
     */
    private AnalysisResult buildResult(Map<String, Object> agentResult,
                                        String resumeFilename,
                                        String resumeText,
                                        String jdText) {
        AnalysisResult result = new AnalysisResult();
        result.setResumeFilename(resumeFilename);
        result.setMatchScore((Integer) agentResult.get("matchScore"));
        result.setMatchLevel((String) agentResult.get("matchLevel"));
        result.setJobTitle((String) agentResult.get("jobTitle"));
        result.setOverallSummary((String) agentResult.get("overallSummary"));
        result.setResumeText(resumeText);
        result.setJdText(jdText);

        // Serialize lists back to JSON strings for storage
        try {
            result.setMatchedSkills(objectMapper.writeValueAsString(agentResult.get("matchedSkills")));
            result.setMissingSkills(objectMapper.writeValueAsString(agentResult.get("missingSkills")));
            result.setRewriteSuggestions(objectMapper.writeValueAsString(agentResult.get("rewriteSuggestions")));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing agent result", e);
        }

        return result;
    }

    /**
     * Fetch all past analyses for the history page.
     */
    public List<AnalysisResult> getAllResults() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Fetch a single analysis by ID.
     */
    public AnalysisResult getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Analysis not found: " + id));
    }
}
