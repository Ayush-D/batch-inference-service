package com.example.batch.model;

/**
 * Lifecycle of a batch job.
 *
 * <ul>
 *   <li>{@code PENDING}   - accepted, not yet started on the worker pool.</li>
 *   <li>{@code RUNNING}   - prompts are being processed.</li>
 *   <li>{@code COMPLETED} - every prompt finished (some may individually have failed).</li>
 *   <li>{@code FAILED}    - the batch itself failed (e.g. a fatal error before/while running).</li>
 * </ul>
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
