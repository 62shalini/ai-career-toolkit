package com.shalini.resumeanalyzer.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shalini.resumeanalyzer.model.AnalysisResult;
import com.shalini.resumeanalyzer.service.AnalyzerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AnalyzerController {

    private final AnalyzerService analyzerService;
    private final ObjectMapper objectMapper;

    /**
     * GET / → Show the upload form
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }

    /**
     * POST /analyze → Run the full RAG + Agent pipeline
     *
     * Accepts:
     *   - resumeFile: PDF upload (required)
     *   - jdText: pasted JD text (optional)
     *   - jdFile: JD as PDF upload (optional, used if jdText is empty)
     */
    @PostMapping("/analyze")
    public String analyze(
            @RequestParam("resumeFile") MultipartFile resumeFile,
            @RequestParam(value = "jdText", required = false) String jdText,
            @RequestParam(value = "jdFile", required = false) MultipartFile jdFile,
            Model model) {

        try {
            // Validate inputs
            if (resumeFile.isEmpty()) {
                model.addAttribute("error", "Please upload your resume PDF.");
                return "index";
            }

            boolean hasJdText = jdText != null && !jdText.isBlank();
            boolean hasJdFile = jdFile != null && !jdFile.isEmpty();

            if (!hasJdText && !hasJdFile) {
                model.addAttribute("error", "Please provide a job description (text or PDF).");
                return "index";
            }

            // Run the pipeline
            AnalysisResult result = analyzerService.analyze(resumeFile, jdText, jdFile);

            // Parse stored JSON strings back to lists for Thymeleaf
            List<String> matchedSkills = objectMapper.readValue(
                result.getMatchedSkills(), new TypeReference<List<String>>() {});
            List<String> missingSkills = objectMapper.readValue(
                result.getMissingSkills(), new TypeReference<List<String>>() {});
            List<Map<String, String>> rewriteSuggestions = objectMapper.readValue(
                result.getRewriteSuggestions(), new TypeReference<List<Map<String, String>>>() {});

            // Pass data to Thymeleaf template
            model.addAttribute("result", result);
            model.addAttribute("matchedSkills", matchedSkills);
            model.addAttribute("missingSkills", missingSkills);
            model.addAttribute("rewriteSuggestions", rewriteSuggestions);

            return "result";

        } catch (Exception e) {
            model.addAttribute("error", "Analysis failed: " + e.getMessage());
            return "index";
        }
    }

    /**
     * GET /history → Show all past analyses
     */
    @GetMapping("/history")
    public String history(Model model) {
        List<AnalysisResult> results = analyzerService.getAllResults();
        model.addAttribute("results", results);
        return "history";
    }

    /**
     * GET /result/{id} → View a past analysis by ID
     */
    @GetMapping("/result/{id}")
    public String viewResult(@PathVariable Long id, Model model) {
        try {
            AnalysisResult result = analyzerService.getById(id);

            List<String> matchedSkills = objectMapper.readValue(
                result.getMatchedSkills(), new TypeReference<List<String>>() {});
            List<String> missingSkills = objectMapper.readValue(
                result.getMissingSkills(), new TypeReference<List<String>>() {});
            List<Map<String, String>> rewriteSuggestions = objectMapper.readValue(
                result.getRewriteSuggestions(), new TypeReference<List<Map<String, String>>>() {});

            model.addAttribute("result", result);
            model.addAttribute("matchedSkills", matchedSkills);
            model.addAttribute("missingSkills", missingSkills);
            model.addAttribute("rewriteSuggestions", rewriteSuggestions);

            return "result";
        } catch (Exception e) {
            model.addAttribute("error", "Result not found.");
            return "history";
        }
    }
}
