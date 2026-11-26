package com.ferro.catalog.application.port;

import java.util.UUID;

import com.ferro.catalog.domain.ExerciseStats;

public interface ExerciseStatsRepository {

    ExerciseStats statsFor(UUID userId, UUID exerciseId);
}
