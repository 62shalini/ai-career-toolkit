package com.shalini.resumeanalyzer.repository;

import com.shalini.resumeanalyzer.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    // Get all past analyses sorted by newest first
    List<AnalysisResult> findAllByOrderByCreatedAtDesc();

    // Find by match level
    List<AnalysisResult> findByMatchLevel(String matchLevel);
}
