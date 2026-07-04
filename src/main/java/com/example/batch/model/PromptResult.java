package com.example.batch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One prompt's outcome, persisted to the {@code prompt_results} table.
 * Rich on purpose: it records not just the answer but how many attempts it took,
 * whether it succeeded, any error, and how long it ran.
 */
@Entity
@Table(name = "prompt_results")
public class PromptResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobId;

    // "text" maps cleanly on both H2 and PostgreSQL. (Plain @Lob on a String
    // becomes a Postgres large-object OID, which fails to read back.)
    @Column(nullable = false, columnDefinition = "text")
    private String prompt;

    @Column(columnDefinition = "text")
    private String response;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false)
    private int attempts;

    private String error;

    @Column(nullable = false)
    private long durationMs;

    protected PromptResult() {
        // required by JPA
    }

    private PromptResult(String jobId, String prompt, String response, boolean success,
                         int attempts, String error, long durationMs) {
        this.jobId = jobId;
        this.prompt = prompt;
        this.response = response;
        this.success = success;
        this.attempts = attempts;
        this.error = error;
        this.durationMs = durationMs;
    }

    public static PromptResult success(String jobId, String prompt, String response,
                                       int attempts, long durationMs) {
        return new PromptResult(jobId, prompt, response, true, attempts, null, durationMs);
    }

    public static PromptResult failure(String jobId, String prompt, String error,
                                       int attempts, long durationMs) {
        return new PromptResult(jobId, prompt, null, false, attempts, error, durationMs);
    }

    public Long getId() {
        return id;
    }

    public String getJobId() {
        return jobId;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getResponse() {
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getError() {
        return error;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
