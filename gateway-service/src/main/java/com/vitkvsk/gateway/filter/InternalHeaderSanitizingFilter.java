package com.vitkvsk.gateway.filter;

import com.vitkvsk.gateway.config.GatewaySecurityConfig;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class InternalHeaderSanitizingFilter implements GlobalFilter, Ordered {

    private static final String INTERNAL_TOKEN_HEADER="X-Internal-Service-Token";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .headers(h -> h.remove(INTERNAL_TOKEN_HEADER))
                        .build())
                .build());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
