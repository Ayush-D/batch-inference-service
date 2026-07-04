package com.example.batch.client;

import java.util.concurrent.ThreadLocalRandom;

import com.example.batch.config.AppProperties;
import org.springframework.stereotype.Component;

/**
 * Stand-in for a real, rate-limited AI inference endpoint.
 *
 * <p>It sleeps briefly to imitate network latency and, with a configurable
 * probability, throws {@link RateLimitException} to imitate an HTTP 429. This is
 * what lets us exercise the worker retry / back-off logic without a real service.
 */
@Component
public class MockInferenceClient {

    private final AppProperties props;

    public MockInferenceClient(AppProperties props) {
        this.props = props;
    }

    /**
     * @throws RateLimitException   to simulate a 429 response
     * @throws InterruptedException if the simulated latency sleep is interrupted
     */
    public String infer(String prompt) throws InterruptedException {
        Thread.sleep(props.getSimulatedLatencyMs());

        if (ThreadLocalRandom.current().nextDouble() < props.getRateLimitProbability()) {
            throw new RateLimitException("429 Too Many Requests");
        }

        return "response for: " + prompt;
    }
}
