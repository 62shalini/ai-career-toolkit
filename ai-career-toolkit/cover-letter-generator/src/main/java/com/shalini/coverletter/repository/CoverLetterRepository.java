package com.shalini.coverletter.repository;

import com.shalini.coverletter.model.CoverLetter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CoverLetterRepository extends JpaRepository<CoverLetter, Long> {
    List<CoverLetter> findAllByOrderByCreatedAtDesc();
    List<CoverLetter> findByCandidateEmail(String email);
}
