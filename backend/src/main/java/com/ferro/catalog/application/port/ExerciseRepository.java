package com.ferro.catalog.application.port;

import java.util.Optional;
import java.util.UUID;

import com.ferro.catalog.domain.Equipment;
import com.ferro.catalog.domain.Exercise;
import com.ferro.catalog.domain.MuscleGroup;
import com.ferro.catalog.domain.PageResult;

public interface ExerciseRepository {

    PageResult<Exercise> search(
            UUID userId, String query, MuscleGroup muscleGroup, Equipment equipment, int page, int size);

    Optional<Exercise> findById(UUID id);

    Exercise save(Exercise exercise);

    long countCustomByOwner(UUID ownerId);

    boolean hasHistory(UUID exerciseId);
}
