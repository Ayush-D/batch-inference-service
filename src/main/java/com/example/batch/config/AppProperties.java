package com.example.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable settings for the batch service. Every field can be overridden by an
 * environment variable (e.g. app.pool-size -> APP_POOL_SIZE) so the same build
 * runs on a laptop and on a small cloud instance without code changes.
 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Number of worker threads processing prompts at the same time. */
    private int poolSize = 8;

    /** Max prompts allowed to wait in the queue before backpressure kicks in. */
    private int queueCapacity = 100;

    /** How many times a single prompt is attempted before it is marked failed. */
    private int maxRetries = 3;

    /** Base delay for exponential back-off after a 429 (milliseconds). */
    private long baseBackoffMs = 200;

    /** Probability (0..1) that the mock endpoint returns HTTP 429. */
    private double rateLimitProbability = 0.2;

    /** Fake processing latency of the mock endpoint (milliseconds). */
    private long simulatedLatencyMs = 50;

    /** Directory for the optional results-{jobId}.json export. */
    private String outputDir = "output";

    /** Whether to also write the aggregated results to a JSON file. */
    private boolean writeJson = true;

    /** How long finished jobs stay in memory before cleanup (minutes). */
    private long jobRetentionMinutes = 30;

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getBaseBackoffMs() {
        return baseBackoffMs;
    }

    public void setBaseBackoffMs(long baseBackoffMs) {
        this.baseBackoffMs = baseBackoffMs;
    }

    public double getRateLimitProbability() {
        return rateLimitProbability;
    }

    public void setRateLimitProbability(double rateLimitProbability) {
        this.rateLimitProbability = rateLimitProbability;
    }

    public long getSimulatedLatencyMs() {
        return simulatedLatencyMs;
    }

    public void setSimulatedLatencyMs(long simulatedLatencyMs) {
        this.simulatedLatencyMs = simulatedLatencyMs;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isWriteJson() {
        return writeJson;
    }

    public void setWriteJson(boolean writeJson) {
        this.writeJson = writeJson;
    }

    public long getJobRetentionMinutes() {
        return jobRetentionMinutes;
    }

    public void setJobRetentionMinutes(long jobRetentionMinutes) {
        this.jobRetentionMinutes = jobRetentionMinutes;
    }
}
