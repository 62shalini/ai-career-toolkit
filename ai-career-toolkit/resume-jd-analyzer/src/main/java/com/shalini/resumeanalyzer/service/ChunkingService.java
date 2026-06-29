package com.shalini.resumeanalyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingService {

    @Value("${chunking.size:500}")
    private int chunkSize;          // characters per chunk (approx 100-120 tokens)

    @Value("${chunking.overlap:50}")
    private int overlapSize;        // overlap between consecutive chunks

    /**
     * Splits a long text into overlapping chunks.
     * 
     * Why overlapping? So that important sentences at chunk boundaries
     * are not cut off and lost — they appear in both the current and next chunk.
     *
     * Example: chunkSize=500, overlap=50
     * Chunk 1: chars 0   to 500
     * Chunk 2: chars 450 to 950  (50 chars overlap from chunk 1)
     * Chunk 3: chars 900 to 1400
     */
    public List<String> chunk(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        // First, try to split on paragraph boundaries for cleaner chunks
        String[] paragraphs = text.split("\n\n");

        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // If adding this paragraph stays within limit, add it
            if (currentChunk.length() + paragraph.length() <= chunkSize) {
                if (currentChunk.length() > 0) currentChunk.append("\n\n");
                currentChunk.append(paragraph);
            } else {
                // Save current chunk if it has content
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }

                // If paragraph itself is bigger than chunk size, split by sentences
                if (paragraph.length() > chunkSize) {
                    List<String> sentenceChunks = splitBySentences(paragraph);
                    chunks.addAll(sentenceChunks);
                    currentChunk = new StringBuilder();
                } else {
                    // Start fresh with overlap from previous chunk
                    String overlap = getOverlap(currentChunk.toString());
                    currentChunk = new StringBuilder(overlap);
                    if (currentChunk.length() > 0) currentChunk.append("\n\n");
                    currentChunk.append(paragraph);
                }
            }
        }

        // Add the last remaining chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * Fallback: split a long paragraph by sentence boundaries.
     */
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

    /**
     * Returns the last `overlapSize` characters of the current chunk
     * to carry forward as context into the next chunk.
     */
    private String getOverlap(String text) {
        if (text.length() <= overlapSize) return text;
        return text.substring(text.length() - overlapSize);
    }
}
