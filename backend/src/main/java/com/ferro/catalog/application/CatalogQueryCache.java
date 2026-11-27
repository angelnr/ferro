package com.ferro.catalog.application;

import java.time.Duration;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.ferro.catalog.domain.Equipment;
import com.ferro.catalog.domain.Exercise;
import com.ferro.catalog.domain.MuscleGroup;
import com.ferro.catalog.domain.PageResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/** RNF-53: caché en memoria de búsquedas de catálogo con TTL de 10 minutos. */
@Component
public class CatalogQueryCache {

    private final Cache<String, PageResult<Exercise>> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(1_000)
            .build();

    public PageResult<Exercise> get(
            UUID userId,
            String query,
            MuscleGroup muscle,
            Equipment equipment,
            int page,
            int size,
            java.util.function.Supplier<PageResult<Exercise>> loader) {
        String key = String.join(
                "|",
                userId.toString(),
                query == null ? "" : query,
                muscle == null ? "" : muscle.name(),
                equipment == null ? "" : equipment.name(),
                Integer.toString(page),
                Integer.toString(size));
        return cache.get(key, ignored -> loader.get());
    }

    public void invalidate() {
        cache.invalidateAll();
    }
}
