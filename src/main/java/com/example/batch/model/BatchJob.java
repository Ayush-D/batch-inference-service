package com.example.batch.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory record of a running batch. Kept out of the database on purpose:
 * status polling hits this (cheap, thread-safe counters) rather than the DB.
 * The durable results live in the {@code prompt_results} table.
 */
public class BatchJob {

    private final String id;
    private final int total;
    private final AtomicInteger completed = new AtomicInteger(0);
    private final AtomicInteger failed = new AtomicInteger(0);
    private final Instant createdAt = Instant.now();

    private volatile JobStatus status = JobStatus.PENDING;
    private volatile Instant finishedAt;

    public BatchJob(String id, int total) {
        this.id = id;
        this.total = total;
    }

    public int incrementCompleted() {
        return completed.incrementAndGet();
    }

    public int incrementFailed() {
        return failed.incrementAndGet();
    }

    /** True once every prompt has produced a result (success or failure). */
    public boolean isAllProcessed() {
        return completed.get() + failed.get() >= total;
    }

    public String getId() {
        return id;
    }

    public int getTotal() {
        return total;
    }

    public int getCompleted() {
        return completed.get();
    }

    public int getFailed() {
        return failed.get();
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
