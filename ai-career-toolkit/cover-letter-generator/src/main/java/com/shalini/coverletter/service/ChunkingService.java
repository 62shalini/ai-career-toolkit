package com.shalini.coverletter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    @Value("${chunking.size:500}")
    private int chunkSize;

    @Value("${chunking.overlap:50}")
    private int overlapSize;

    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        String[] paragraphs = text.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            if (currentChunk.length() + paragraph.length() <= chunkSize) {
                if (currentChunk.length() > 0) currentChunk.append("\n\n");
                currentChunk.append(paragraph);
            } else {
                if (currentChunk.length() > 0) chunks.add(currentChunk.toString().trim());
                if (paragraph.length() > chunkSize) {
                    chunks.addAll(splitBySentences(paragraph));
                    currentChunk = new StringBuilder();
                } else {
                    String overlap = getOverlap(currentChunk.toString());
                    currentChunk = new StringBuilder(overlap);
                    if (currentChunk.length() > 0) currentChunk.append("\n\n");
                    currentChunk.append(paragraph);
                }
            }
        }
        if (currentChunk.length() > 0) chunks.add(currentChunk.toString().trim());
        return chunks;
    }

    private List<String> splitBySentences(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            if (current.length() + sentence.length() <= chunkSize) {
                if (current.length() > 0) current.append(" ");
                current.append(sentence);
            } else {
                if (current.length() > 0) chunks.add(current.toString().trim());
                current = new StringBuilder(sentence);
            }
        }
        if (current.length() > 0) chunks.add(current.toString().trim());
        return chunks;
    }

    private String getOverlap(String text) {
        if (text.length() <= overlapSize) return text;
        return text.substring(text.length() - overlapSize);
    }
}
