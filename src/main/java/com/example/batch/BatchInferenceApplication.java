package com.example.batch;

import com.example.batch.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class BatchInferenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BatchInferenceApplication.class, args);
    }
}
