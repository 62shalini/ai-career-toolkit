package com.shalini.coverletter.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.io.font.constants.StandardFonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfExportService {

    /**
     * Converts a cover letter string into a professionally formatted PDF.
     * Returns byte[] so the controller can stream it as a download.
     *
     * Layout:
     * - Candidate name as header (large, bold)
     * - Contact info line (email | phone)
     * - Horizontal rule (visual separator)
     * - Cover letter body text (normal font, justified)
     */
    public byte[] exportToPdf(String candidateName,
                               String candidateEmail,
                               String candidatePhone,
                               String coverLetterText) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Fonts
        PdfFont boldFont   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        // Set margins (72pt = 1 inch)
        document.setMargins(72, 72, 72, 72);

        // ── Header: Candidate Name ───────────────────────────
        Paragraph namePara = new Paragraph(candidateName)
            .setFont(boldFont)
            .setFontSize(20)
            .setFontColor(ColorConstants.BLACK)
            .setTextAlignment(TextAlignment.LEFT)
            .setMarginBottom(4);
        document.add(namePara);

        // ── Contact info line ────────────────────────────────
        String contactLine = candidateEmail + "  |  " + candidatePhone;
        Paragraph contactPara = new Paragraph(contactLine)
            .setFont(normalFont)
            .setFontSize(10)
            .setFontColor(ColorConstants.GRAY)
            .setTextAlignment(TextAlignment.LEFT)
            .setMarginBottom(16);
        document.add(contactPara);

        // ── Visual separator ─────────────────────────────────
        Paragraph separator = new Paragraph("──────────────────────────────────────────────────")
            .setFont(normalFont)
            .setFontSize(10)
            .setFontColor(ColorConstants.LIGHT_GRAY)
            .setMarginBottom(20);
        document.add(separator);

        // ── Cover letter body ────────────────────────────────
        // Split on newlines to preserve paragraph structure
        String[] lines = coverLetterText.split("\n");
        StringBuilder paragraphBuffer = new StringBuilder();

        for (String line : lines) {
            if (line.isBlank()) {
                // Empty line = paragraph break
                if (paragraphBuffer.length() > 0) {
                    document.add(makeBodyParagraph(paragraphBuffer.toString(), normalFont));
                    paragraphBuffer = new StringBuilder();
                }
            } else {
                if (paragraphBuffer.length() > 0) paragraphBuffer.append(" ");
                paragraphBuffer.append(line.trim());
            }
        }

        // Flush any remaining text
        if (paragraphBuffer.length() > 0) {
            document.add(makeBodyParagraph(paragraphBuffer.toString(), normalFont));
        }

        document.close();
        return outputStream.toByteArray();
    }

    private Paragraph makeBodyParagraph(String text, PdfFont font) {
        return new Paragraph(text)
            .setFont(font)
            .setFontSize(11)
            .setFontColor(ColorConstants.BLACK)
            .setTextAlignment(TextAlignment.JUSTIFIED)
            .setLineHeight(1.5f)
            .setMarginBottom(12);
    }
}
