package com.example.batch.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.example.batch.dto.BatchAcceptedResponse;
import com.example.batch.dto.JobStatusResponse;
import com.example.batch.model.BatchJob;
import com.example.batch.model.PromptResult;
import com.example.batch.service.BatchProcessingService;
import com.example.batch.store.JobStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchProcessingService service;
    private final JobStore jobStore;
    private final ObjectMapper objectMapper;

    public BatchController(BatchProcessingService service, JobStore jobStore, ObjectMapper objectMapper) {
        this.service = service;
        this.jobStore = jobStore;
        this.objectMapper = objectMapper;
    }

    /** Ingest a batch via API upload: body is a JSON array of prompt strings. */
    @PostMapping
    public ResponseEntity<BatchAcceptedResponse> submit(@RequestBody List<String> prompts) {
        if (prompts == null || prompts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompts must be a non-empty JSON array");
        }
        BatchJob job = service.submitBatch(prompts);
        return ResponseEntity.accepted()
                .body(new BatchAcceptedResponse(job.getId(), job.getTotal(), job.getStatus()));
    }

    /** Ingest a batch via file read: loads the bundled sample-prompts.json. */
    @PostMapping("/from-file")
    public ResponseEntity<BatchAcceptedResponse> submitFromFile() throws IOException {
        try (InputStream in = new ClassPathResource("sample-prompts.json").getInputStream()) {
            List<String> prompts = objectMapper.readValue(in, new TypeReference<>() {});
            BatchJob job = service.submitBatch(prompts);
            return ResponseEntity.accepted()
                    .body(new BatchAcceptedResponse(job.getId(), job.getTotal(), job.getStatus()));
        }
    }

    /** Job Status API: real-time progress (e.g. 400/1000 completed). */
    @GetMapping("/{id}")
    public JobStatusResponse status(@PathVariable String id) {
        BatchJob job = jobStore.get(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown job id: " + id));
        return JobStatusResponse.from(job);
    }

    /** Aggregated results, read from the database. */
    @GetMapping("/{id}/results")
    public List<PromptResult> results(@PathVariable String id) {
        if (jobStore.get(id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown job id: " + id);
        }
        return service.getResults(id);
    }
}
