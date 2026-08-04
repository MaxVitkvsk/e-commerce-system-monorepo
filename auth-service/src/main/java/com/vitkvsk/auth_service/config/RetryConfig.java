package com.vitkvsk.auth_service.config;

public final class RetryConfig {
    private RetryConfig() {}
    public static final int MAX_RETRIES = 2;
    public static final long DELAY_MS = 500;
    public static final double MULTIPLIER = 2.0;
    public static final long JITTER_MS = 250;
}
