package com.example.batch.store;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.example.batch.config.AppProperties;
import com.example.batch.model.BatchJob;
import com.example.batch.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * In-memory registry of jobs, keyed by id. Powers fast status polling.
 *
 * <p>Production thinking: without cleanup this map would grow forever. A
 * scheduled sweep evicts finished jobs older than the configured retention. The
 * durable results remain in the database, so eviction only drops the live
 * progress counters, not the data.
 */
@Component
public class JobStore {

    private static final Logger log = LoggerFactory.getLogger(JobStore.class);

    private final ConcurrentHashMap<String, BatchJob> jobs = new ConcurrentHashMap<>();
    private final AppProperties props;

    public JobStore(AppProperties props) {
        this.props = props;
    }

    public void put(BatchJob job) {
        jobs.put(job.getId(), job);
    }

    public Optional<BatchJob> get(String id) {
        return Optional.ofNullable(jobs.get(id));
    }

    public int size() {
        return jobs.size();
    }

    @Scheduled(fixedDelayString = "PT5M")
    public void evictOldJobs() {
        Duration retention = Duration.ofMinutes(props.getJobRetentionMinutes());
        Instant cutoff = Instant.now().minus(retention);
        int before = jobs.size();
        jobs.values().removeIf(job -> isFinished(job.getStatus())
                && job.getFinishedAt() != null
                && job.getFinishedAt().isBefore(cutoff));
        int removed = before - jobs.size();
        if (removed > 0) {
            log.info("Evicted {} finished job(s) older than {} minutes", removed,
                    props.getJobRetentionMinutes());
        }
    }

    private boolean isFinished(JobStatus status) {
        return status == JobStatus.COMPLETED || status == JobStatus.FAILED;
    }
}
