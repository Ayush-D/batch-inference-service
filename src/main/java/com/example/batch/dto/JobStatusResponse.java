package com.example.batch.dto;

import com.example.batch.model.BatchJob;
import com.example.batch.model.JobStatus;

/** Real-time progress snapshot for the Job Status API. */
public record JobStatusResponse(
        String jobId,
        JobStatus status,
        int total,
        int completed,
        int failed) {

    public static JobStatusResponse from(BatchJob job) {
        return new JobStatusResponse(
                job.getId(),
                job.getStatus(),
                job.getTotal(),
                job.getCompleted(),
                job.getFailed());
    }
}
