package com.vitkvsk.payment_service.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomNumberClient {

    private final RestClient randomRestClient;

    @Retry(name = "remote")
    public int getRandomNumber() {
        String body = randomRestClient
                .get()
                .retrieve()
                .body(String.class);

        return Integer.parseInt(body.trim());
    }
}