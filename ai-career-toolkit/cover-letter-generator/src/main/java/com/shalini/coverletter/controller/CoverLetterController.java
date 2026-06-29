package com.shalini.coverletter.controller;

import com.shalini.coverletter.model.CoverLetter;
import com.shalini.coverletter.service.CoverLetterService;
import com.shalini.coverletter.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CoverLetterController {

    private final CoverLetterService coverLetterService;
    private final PdfExportService pdfExportService;

    @GetMapping("/")
    public String home() { return "index"; }

    @PostMapping("/generate")
    public String generate(
            @RequestParam("resumeFile") MultipartFile resumeFile,
            @RequestParam(value = "jdText", required = false) String jdText,
            @RequestParam(value = "jdFile", required = false) MultipartFile jdFile,
            @RequestParam("candidateName") String candidateName,
            @RequestParam("candidateEmail") String candidateEmail,
            @RequestParam("candidatePhone") String candidatePhone,
            @RequestParam("companyName") String companyName,
            @RequestParam("jobTitle") String jobTitle,
            @RequestParam(value = "hiringManagerName", required = false) String hiringManagerName,
            @RequestParam(value = "tone", defaultValue = "Professional") String tone,
            Model model) {
        try {
            if (resumeFile.isEmpty()) { model.addAttribute("error", "Please upload your resume PDF."); return "index"; }
            boolean hasJdText = jdText != null && !jdText.isBlank();
            boolean hasJdFile = jdFile != null && !jdFile.isEmpty();
            if (!hasJdText && !hasJdFile) { model.addAttribute("error", "Please provide a job description."); return "index"; }
            CoverLetter result = coverLetterService.generate(resumeFile, jdText, jdFile, candidateName, candidateEmail, candidatePhone, companyName, jobTitle, hiringManagerName, tone);
            model.addAttribute("coverLetter", result);
            return "result";
        } catch (Exception e) {
            model.addAttribute("error", "Generation failed: " + e.getMessage());
            return "index";
        }
    }

    @GetMapping("/history")
    public String history(Model model) {
        model.addAttribute("letters", coverLetterService.getAll());
        return "history";
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable Long id, Model model) {
        try { model.addAttribute("coverLetter", coverLetterService.getById(id)); return "result"; }
        catch (Exception e) { model.addAttribute("error", "Not found."); return "history"; }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        try {
            CoverLetter letter = coverLetterService.getById(id);
            byte[] pdf = pdfExportService.exportToPdf(letter.getCandidateName(), letter.getCandidateEmail(), letter.getCandidatePhone(), letter.getCoverLetterText());
            String filename = "cover-letter-" + letter.getCompanyName().replaceAll("\\s+", "-").toLowerCase() + ".pdf";
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
        } catch (Exception e) { return ResponseEntity.internalServerError().build(); }
    }
}
