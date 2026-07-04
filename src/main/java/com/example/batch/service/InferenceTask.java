package com.example.batch.service;

import com.example.batch.client.MockInferenceClient;
import com.example.batch.client.RateLimitException;
import com.example.batch.config.AppProperties;
import com.example.batch.model.BatchJob;
import com.example.batch.model.PromptResult;
import com.example.batch.repository.PromptResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes a single prompt on a worker thread.
 *
 * <p>Retry policy: try up to {@code maxRetries} times. On a 429
 * ({@link RateLimitException}) sleep with exponential back-off
 * ({@code baseBackoffMs * 2^(attempt-1)}) then retry. A prompt is only marked
 * failed after all attempts are exhausted, so a single bad prompt never drops
 * the whole batch.
 *
 * <p>Interrupt handling: if a sleep is interrupted we restore the interrupt flag
 * (so the pool stays responsive to shutdown) and fail this task cleanly instead
 * of swallowing the exception.
 */
public class InferenceTask implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(InferenceTask.class);

    private final BatchJob job;
    private final String prompt;
    private final MockInferenceClient client;
    private final AppProperties props;
    private final PromptResultRepository repository;

    public InferenceTask(BatchJob job, String prompt, MockInferenceClient client,
                         AppProperties props, PromptResultRepository repository) {
        this.job = job;
        this.prompt = prompt;
        this.client = client;
        this.props = props;
        this.repository = repository;
    }

    @Override
    public void run() {
        PromptResult result = process();
        repository.save(result);
        if (result.isSuccess()) {
            job.incrementCompleted();
        } else {
            job.incrementFailed();
        }
    }

    /**
     * Runs the retry loop and returns the outcome. Package-visible so tests can
     * assert on the number of attempts and the back-off behaviour directly.
     */
    PromptResult process() {
        long start = System.currentTimeMillis();
        int maxRetries = props.getMaxRetries();
        String lastError = "unknown error";

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String response = client.infer(prompt);
                return PromptResult.success(job.getId(), prompt, response, attempt,
                        System.currentTimeMillis() - start);
            } catch (RateLimitException e) {
                lastError = e.getMessage();
                log.debug("Job {} prompt got 429 on attempt {}/{}", job.getId(), attempt, maxRetries);
                if (attempt < maxRetries) {
                    long delay = props.getBaseBackoffMs() * (1L << (attempt - 1));
                    if (!sleep(delay)) {
                        return PromptResult.failure(job.getId(), prompt,
                                "Interrupted during back-off", attempt,
                                System.currentTimeMillis() - start);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return PromptResult.failure(job.getId(), prompt, "Interrupted", attempt,
                        System.currentTimeMillis() - start);
            }
        }

        return PromptResult.failure(job.getId(), prompt, lastError, maxRetries,
                System.currentTimeMillis() - start);
    }

    /** @return true if the sleep completed, false if it was interrupted. */
    private boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
