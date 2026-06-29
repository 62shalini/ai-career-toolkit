package com.shalini.coverletter.service;

import com.shalini.coverletter.model.Chunk;
import com.shalini.coverletter.model.CoverLetter;
import com.shalini.coverletter.repository.CoverLetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoverLetterService {

    private final PdfParserService pdfParserService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final CoverLetterAgentService agentService;
    private final CoverLetterRepository repository;

    private static final int TOP_K = 6;

    /**
     * FULL PIPELINE — called by the controller on form submit
     *
     * 1. Parse resume PDF → plain text
     * 2. Parse JD (text or PDF) → plain text
     * 3. Chunk the JD into overlapping segments
     * 4. Embed all JD chunks into vectors
     * 5. Embed the resume as a query vector
     * 6. Retrieve top-K JD chunks most similar to resume (RAG retrieval)
     * 7. Pass everything to Claude agent → generate cover letter
     * 8. Save to MySQL
     * 9. Return saved entity
     *
     * Why RAG here? Because instead of dumping the whole JD into Claude
     * (which wastes context and can confuse it), we retrieve only the
     * most relevant JD sections — those that match the candidate's background.
     * Claude then writes a letter that naturally mirrors those sections' language.
     */
    public CoverLetter generate(
            MultipartFile resumeFile,
            String jdText,
            MultipartFile jdFile,
            String candidateName,
            String candidateEmail,
            String candidatePhone,
            String companyName,
            String jobTitle,
            String hiringManagerName,
            String tone) throws IOException {

        // Step 1: Parse resume
        String resumeText = pdfParserService.extractText(resumeFile);

        // Step 2: Parse JD
        String jobDescText;
        if (jdFile != null && !jdFile.isEmpty()) {
            jobDescText = pdfParserService.extractText(jdFile);
        } else {
            jobDescText = pdfParserService.extractFromString(jdText);
        }

        // Step 3 & 4: Chunk + embed JD
        List<String> jdChunkTexts = chunkingService.chunk(jobDescText);
        List<Chunk> jdChunks = embeddingService.embedChunks(jdChunkTexts, "jd");

        // Step 5: Embed resume (truncate to stay within token limit)
        String resumeQuery = resumeText.length() > 4000
            ? resumeText.substring(0, 4000) : resumeText;
        List<Double> resumeEmbedding = embeddingService.getEmbedding(resumeQuery);

        // Step 6: Retrieve top-K relevant JD chunks
        List<Chunk> topJdChunks = embeddingService.retrieveTopK(resumeEmbedding, jdChunks, TOP_K);

        // Store which JD context was used (useful for transparency / debugging)
        String jdContextUsed = topJdChunks.stream()
            .map(Chunk::getText)
            .collect(Collectors.joining("\n\n---\n\n"));

        // Step 7: Generate cover letter via Claude
        String coverLetterText = agentService.generateCoverLetter(
            resumeText, topJdChunks,
            candidateName, candidateEmail, candidatePhone,
            companyName, jobTitle, hiringManagerName, tone
        );

        // Step 8: Save to MySQL
        CoverLetter entity = new CoverLetter();
        entity.setCandidateName(candidateName);
        entity.setCandidateEmail(candidateEmail);
        entity.setCandidatePhone(candidatePhone);
        entity.setResumeFilename(resumeFile.getOriginalFilename());
        entity.setCompanyName(companyName);
        entity.setJobTitle(jobTitle);
        entity.setHiringManagerName(hiringManagerName);
        entity.setTone(tone);
        entity.setCoverLetterText(coverLetterText);
        entity.setJdContextUsed(jdContextUsed);

        return repository.save(entity);
    }

    public List<CoverLetter> getAll() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public CoverLetter getById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cover letter not found: " + id));
    }
}
