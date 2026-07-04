package com.example.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import com.example.batch.model.BatchJob;
import com.example.batch.model.JobStatus;
import com.example.batch.repository.PromptResultRepository;
import com.example.batch.service.BatchProcessingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@TestPropertySource(properties = {
        "app.simulated-latency-ms=1",
        "app.base-backoff-ms=1",
        "app.max-retries=5",
        "app.rate-limit-probability=0.3",
        "app.write-json=false"
})
class BatchProcessingIntegrationTest {

    @Autowired
    private BatchProcessingService service;

    @Autowired
    private PromptResultRepository repository;

    @Autowired
    private WebApplicationContext webContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void processesEntireBatchDespiteRateLimiting() {
        List<String> prompts = List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
                "k", "l", "m", "n", "o", "p", "q", "r", "s", "t");

        BatchJob job = service.submitBatch(prompts);

        await().atMost(Duration.ofSeconds(20))
                .until(() -> job.getStatus() == JobStatus.COMPLETED);

        assertThat(job.getCompleted() + job.getFailed()).isEqualTo(prompts.size());
        assertThat(repository.findByJobId(job.getId())).hasSize(prompts.size());
    }

    @Test
    void endpointReturns202AndTracksStatus() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();

        MvcResult accepted = mockMvc.perform(post("/api/batches")
                        .contentType("application/json")
                        .content("[\"one\",\"two\",\"three\"]"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.total").value(3))
                .andReturn();

        JsonNode body = objectMapper.readTree(accepted.getResponse().getContentAsString());
        String jobId = body.get("jobId").asText();

        await().atMost(Duration.ofSeconds(20)).until(() ->
                repository.countByJobId(jobId) == 3);

        mockMvc.perform(get("/api/batches/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));

        mockMvc.perform(get("/api/batches/{id}/results", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void unknownJobReturns404() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webContext).build();
        mockMvc.perform(get("/api/batches/{id}", "does-not-exist"))
                .andExpect(status().isNotFound());
    }
}
