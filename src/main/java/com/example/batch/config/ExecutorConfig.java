package com.example.batch.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines the bounded worker pool used to process prompts concurrently.
 *
 * <p>Two limits keep the app from exhausting memory or spawning unbounded
 * threads: a fixed number of threads ({@code poolSize}) and a bounded queue
 * ({@code queueCapacity}). When the queue is full, {@link ThreadPoolExecutor.CallerRunsPolicy}
 * makes the submitting thread run the task itself, which naturally slows down
 * submission (backpressure) instead of piling work up in memory.
 */
@Configuration
public class ExecutorConfig {

    @Bean(name = "inferenceExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor inferenceExecutor(AppProperties props) {
        int poolSize = props.getPoolSize();
        return new ThreadPoolExecutor(
                poolSize,
                poolSize,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(props.getQueueCapacity()),
                new WorkerThreadFactory("inference-worker"),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * A small, separate pool whose only job is to submit a batch's tasks to the
     * worker pool and wait for completion. Keeping this off the request thread is
     * what lets the POST return immediately (202) while work runs in the
     * background, even when backpressure slows submission down.
     */
    @Bean(name = "orchestratorExecutor", destroyMethod = "shutdown")
    public ExecutorService orchestratorExecutor() {
        return Executors.newFixedThreadPool(2, new WorkerThreadFactory("batch-orchestrator"));
    }

    /** Names threads so they are easy to spot in logs / thread dumps. */
    private static final class WorkerThreadFactory implements java.util.concurrent.ThreadFactory {
        private final String prefix;
        private int counter = 0;

        private WorkerThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public synchronized Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + (++counter));
            t.setDaemon(true);
            return t;
        }
    }
}
