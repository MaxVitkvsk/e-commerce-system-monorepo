package com.vitkvsk.payment_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    private final RestClient randomRestClient;

    @Retryable(
            includes = {
                    RestClientResponseException.class,
                    ResourceAccessException.class
            },
            maxRetriesString = "${app.retry.max-attempts:3}",
            delayString = "${app.retry.delay-ms:500}",
            multiplierString = "${app.retry.multiplier:2.0}",
            jitterString = "${app.retry.jitter-ms:100}",
            maxDelayString = "${app.retry.max-delay-ms:10000}"
    )
    public int getRandomNumber() {
        String body = randomRestClient
                .get()
                .retrieve()
                .body(String.class);

        return Integer.parseInt(body.trim());
    }
}