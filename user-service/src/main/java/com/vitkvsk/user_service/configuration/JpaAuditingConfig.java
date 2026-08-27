package com.vitkvsk.user_service.configuration;


import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
