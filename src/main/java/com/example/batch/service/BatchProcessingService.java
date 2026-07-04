package com.example.batch.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import com.example.batch.client.MockInferenceClient;
import com.example.batch.config.AppProperties;
import com.example.batch.model.BatchJob;
import com.example.batch.model.JobStatus;
import com.example.batch.model.PromptResult;
import com.example.batch.repository.PromptResultRepository;
import com.example.batch.store.JobStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Accepts a batch, hands processing to a background orchestrator thread (so the
 * caller gets an immediate acknowledgment), fans the prompts out across the
 * bounded worker pool, persists each result, and finalizes the job.
 */
@Service
public class BatchProcessingService {

    private static final Logger log = LoggerFactory.getLogger(BatchProcessingService.class);

    private final ThreadPoolExecutor workerExecutor;
    private final ExecutorService orchestratorExecutor;
    private final MockInferenceClient client;
    private final AppProperties props;
    private final PromptResultRepository repository;
    private final JobStore jobStore;
    private final ObjectMapper objectMapper;

    public BatchProcessingService(
            @Qualifier("inferenceExecutor") ThreadPoolExecutor workerExecutor,
            @Qualifier("orchestratorExecutor") ExecutorService orchestratorExecutor,
            MockInferenceClient client,
            AppProperties props,
            PromptResultRepository repository,
            JobStore jobStore,
            ObjectMapper objectMapper) {
        this.workerExecutor = workerExecutor;
        this.orchestratorExecutor = orchestratorExecutor;
        this.client = client;
        this.props = props;
        this.repository = repository;
        this.jobStore = jobStore;
        this.objectMapper = objectMapper;
    }

    /** Registers the job and kicks off background processing. Returns immediately. */
    public BatchJob submitBatch(List<String> prompts) {
        String jobId = UUID.randomUUID().toString();
        BatchJob job = new BatchJob(jobId, prompts.size());
        jobStore.put(job);
        log.info("Accepted batch {} with {} prompt(s)", jobId, prompts.size());
        orchestratorExecutor.submit(() -> runBatch(job, prompts));
        return job;
    }

    /** Runs on an orchestrator thread, not the request thread. */
    void runBatch(BatchJob job, List<String> prompts) {
        job.setStatus(JobStatus.RUNNING);
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>(prompts.size());
            for (String prompt : prompts) {
                InferenceTask task = new InferenceTask(job, prompt, client, props, repository);
                futures.add(CompletableFuture.runAsync(task, workerExecutor));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

            if (props.isWriteJson()) {
                writeJsonExport(job);
            }
            job.setStatus(JobStatus.COMPLETED);
            log.info("Batch {} completed: {} ok, {} failed", job.getId(),
                    job.getCompleted(), job.getFailed());
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            log.error("Batch {} failed", job.getId(), e);
        } finally {
            job.setFinishedAt(Instant.now());
        }
    }

    public List<PromptResult> getResults(String jobId) {
        return repository.findByJobId(jobId);
    }

    private void writeJsonExport(BatchJob job) {
        try {
            Path dir = Path.of(props.getOutputDir());
            Files.createDirectories(dir);
            Path file = dir.resolve("results-" + job.getId() + ".json");
            List<PromptResult> results = repository.findByJobId(job.getId());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), results);
            log.info("Wrote {} result(s) to {}", results.size(), file.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Could not write JSON export for job {}: {}", job.getId(), e.getMessage());
        }
    }
}
