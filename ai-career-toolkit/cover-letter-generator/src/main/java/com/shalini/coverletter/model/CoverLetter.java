package com.shalini.coverletter.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "cover_letters")
@Data
@NoArgsConstructor
public class CoverLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Inputs
    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "candidate_email")
    private String candidateEmail;

    @Column(name = "candidate_phone")
    private String candidatePhone;

    @Column(name = "resume_filename")
    private String resumeFilename;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "hiring_manager_name")
    private String hiringManagerName; // optional

    @Column(name = "tone")
    private String tone; // "Professional", "Enthusiastic", "Concise"

    // Generated output
    @Column(name = "cover_letter_text", columnDefinition = "TEXT")
    private String coverLetterText;

    // RAG context used (for debugging / transparency)
    @Column(name = "jd_context_used", columnDefinition = "TEXT")
    private String jdContextUsed;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
