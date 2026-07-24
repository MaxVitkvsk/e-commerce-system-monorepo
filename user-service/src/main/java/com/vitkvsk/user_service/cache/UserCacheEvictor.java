package com.vitkvsk.user_service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCacheEvictor {

    public static final String CACHE_NAME = "usersWithCards";

    private final CacheManager cacheManager;

    public void evict(Long userId) {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict(userId);
            log.debug("Evicted cache '{}' for userId={}", CACHE_NAME, userId);
        } else {
            log.warn("Cache '{}' not found, eviction skipped", CACHE_NAME);
        }
    }

}
