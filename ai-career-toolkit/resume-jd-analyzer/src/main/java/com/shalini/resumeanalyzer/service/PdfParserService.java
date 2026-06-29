package com.shalini.resumeanalyzer.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PdfParserService {

    /**
     * Extracts plain text from an uploaded PDF file.
     * Uses Apache PDFBox 3.x.
     */
    public String extractText(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return cleanText(text);
        }
    }

    /**
     * Accepts raw text directly (when user pastes JD as text).
     */
    public String extractFromString(String rawText) {
        return cleanText(rawText);
    }

    /**
     * Cleans extracted text: removes extra whitespace, blank lines, etc.
     */
    private String cleanText(String text) {
        if (text == null) return "";
        return text
            .replaceAll("\\r\\n", "\n")       // normalize line endings
            .replaceAll("\\r", "\n")
            .replaceAll("[ \\t]+", " ")        // collapse multiple spaces/tabs
            .replaceAll("\\n{3,}", "\n\n")     // collapse 3+ blank lines to 2
            .trim();
    }
}
