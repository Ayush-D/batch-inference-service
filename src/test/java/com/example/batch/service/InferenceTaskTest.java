package com.example.batch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.batch.client.MockInferenceClient;
import com.example.batch.client.RateLimitException;
import com.example.batch.config.AppProperties;
import com.example.batch.model.BatchJob;
import com.example.batch.model.PromptResult;
import com.example.batch.repository.PromptResultRepository;
import org.junit.jupiter.api.Test;

class InferenceTaskTest {

    private AppProperties props(int maxRetries) {
        AppProperties p = new AppProperties();
        p.setMaxRetries(maxRetries);
        p.setBaseBackoffMs(1); // keep tests fast
        return p;
    }

    private InferenceTask task(MockInferenceClient client, AppProperties props) {
        BatchJob job = new BatchJob("job-1", 1);
        PromptResultRepository repo = mock(PromptResultRepository.class);
        return new InferenceTask(job, "hello", client, props, repo);
    }

    @Test
    void retriesOnRateLimitThenSucceeds() throws Exception {
        MockInferenceClient client = mock(MockInferenceClient.class);
        when(client.infer(anyString()))
                .thenThrow(new RateLimitException("429 Too Many Requests"))
                .thenThrow(new RateLimitException("429 Too Many Requests"))
                .thenReturn("ok");

        PromptResult result = task(client, props(3)).process();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResponse()).isEqualTo("ok");
        assertThat(result.getAttempts()).isEqualTo(3);
        verify(client, times(3)).infer(anyString());
    }

    @Test
    void marksFailedAfterExhaustingRetriesWithoutThrowing() throws Exception {
        MockInferenceClient client = mock(MockInferenceClient.class);
        when(client.infer(anyString()))
                .thenThrow(new RateLimitException("429 Too Many Requests"));

        PromptResult result = task(client, props(3)).process();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getAttempts()).isEqualTo(3);
        assertThat(result.getError()).contains("429");
        verify(client, times(3)).infer(anyString());
    }

    @Test
    void restoresInterruptFlagAndFailsCleanlyWhenInterruptedDuringBackoff() throws Exception {
        MockInferenceClient client = mock(MockInferenceClient.class);
        when(client.infer(anyString()))
                .thenThrow(new RateLimitException("429 Too Many Requests"));

        // Pre-set the interrupt flag so the back-off sleep throws immediately.
        Thread.currentThread().interrupt();
        PromptResult result = task(client, props(3)).process();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).contains("Interrupted");
        // Flag must have been restored; interrupted() reads and clears it.
        assertThat(Thread.interrupted()).isTrue();
    }
}
