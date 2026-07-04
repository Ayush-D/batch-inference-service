package com.example.batch.client;

/** Thrown by the mock endpoint to represent an HTTP 429 "Too Many Requests". */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
