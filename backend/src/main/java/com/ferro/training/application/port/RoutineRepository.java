package com.ferro.training.application.port;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ferro.training.domain.Routine;
import com.ferro.training.domain.RoutineSummary;

public interface RoutineRepository {

    List<RoutineSummary> findAll(UUID userId, int page, int size);

    long count(UUID userId);

    boolean nameExists(UUID userId, String name);

    Optional<Routine> find(UUID userId, UUID routineId);

    void create(UUID userId, Routine routine);

    void replace(UUID userId, Routine routine);

    void delete(UUID userId, UUID routineId);
}
