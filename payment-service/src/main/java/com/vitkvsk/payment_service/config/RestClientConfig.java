package com.vitkvsk.payment_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient randomRestClient(
            @Value("${app.random-api.url:https://www.random.org/integers/?num=1&min=1&max=1000&col=1&base=10&format=plain&rnd=new}")
            String url) {
        return RestClient.builder()
                .baseUrl(url)
                .build();
    }
}
