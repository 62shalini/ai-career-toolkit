package com.shalini.resumeanalyzer.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Chunk {
    private String text;
    private List<Double> embedding;
    private String source; // "resume" or "jd"
    private int index;
}
