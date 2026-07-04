package com.example.batch.dto;

import com.example.batch.model.JobStatus;

/** Returned immediately (HTTP 202) when a batch is accepted. */
public record BatchAcceptedResponse(String jobId, int total, JobStatus status) {
}
