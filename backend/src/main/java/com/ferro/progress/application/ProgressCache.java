package com.ferro.progress.application;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ferro.progress.domain.ProgressSummary;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/** Read model cache for the progress panel; invalidated by training events. */
@Component
public class ProgressCache {

    private final Cache<String, ProgressSummary> summaries = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(500)
            .build();

    public ProgressSummary summary(UUID userId, int weeks, java.util.function.Supplier<ProgressSummary> loader) {
        return summaries.get(userId + ":" + weeks, ignored -> loader.get());
    }

    public void invalidate(UUID userId) {
        summaries.asMap().keySet().removeIf(key -> key.startsWith(userId + ":"));
    }
}
