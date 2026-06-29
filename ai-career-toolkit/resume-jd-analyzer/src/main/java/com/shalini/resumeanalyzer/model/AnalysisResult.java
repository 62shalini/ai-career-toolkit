package com.shalini.resumeanalyzer.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "analysis_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resume_filename")
    private String resumeFilename;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "match_level")
    private String matchLevel; // "Strong Match", "Partial Match", "Weak Match"

    @Column(name = "matched_skills", columnDefinition = "TEXT")
    private String matchedSkills; // JSON array as string

    @Column(name = "missing_skills", columnDefinition = "TEXT")
    private String missingSkills; // JSON array as string

    @Column(name = "rewrite_suggestions", columnDefinition = "TEXT")
    private String rewriteSuggestions; // JSON array as string

    @Column(name = "overall_summary", columnDefinition = "TEXT")
    private String overallSummary;

    @Column(name = "resume_text", columnDefinition = "TEXT")
    private String resumeText;

    @Column(name = "jd_text", columnDefinition = "TEXT")
    private String jdText;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
