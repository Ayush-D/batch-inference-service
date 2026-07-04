package com.example.batch.repository;

import java.util.List;

import com.example.batch.model.PromptResult;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data creates the implementation at runtime: save(...) and the derived
 * query findByJobId(...) come for free, no SQL required.
 */
public interface PromptResultRepository extends JpaRepository<PromptResult, Long> {

    List<PromptResult> findByJobId(String jobId);

    long countByJobId(String jobId);
}
