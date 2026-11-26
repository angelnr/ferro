package com.ferro.training.application.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ferro.training.domain.NewPersonalRecord;
import com.ferro.training.domain.PreviousSession;
import com.ferro.training.domain.SessionSet;
import com.ferro.training.domain.SetType;
import com.ferro.training.domain.StaleSession;
import com.ferro.training.domain.WorkoutSession;

public interface TrainingRepository {

    Optional<WorkoutSession> findActive(UUID userId);

    Optional<WorkoutSession> find(UUID userId, UUID sessionId);

    WorkoutSession create(UUID userId, UUID sessionId, String name, Instant now);

    int exerciseCount(UUID sessionId);

    int setCount(UUID sessionExerciseId);

    void addExercise(UUID sessionId, UUID entryId, UUID exerciseId, int position);

    SessionSet addSet(
            UUID entryId,
            UUID setId,
            SetType setType,
            int reps,
            BigDecimal weight,
            Integer rir,
            BigDecimal volume,
            BigDecimal e1rm,
            Instant loggedAt);

    BigDecimal totalVolume(UUID sessionId);

    void finish(UUID sessionId, Instant finishedAt, int durationSeconds, BigDecimal totalVolume);

    void discard(UUID sessionId, Instant now);

    List<NewPersonalRecord> recalculatePersonalRecords(UUID userId, UUID sessionId, Instant now);

    Optional<ExerciseInfo> exercise(UUID exerciseId);

    void updateSession(UUID sessionId, String name, String notes);

    void updateExercise(UUID entryId, Integer restSeconds, Integer supersetGroup, String notes);

    void deleteExercise(UUID entryId);

    void reorderExercises(UUID sessionId, List<UUID> orderedEntryIds);

    void setSupersetGroup(UUID sessionId, List<UUID> entryIds, Integer group);

    void updateSet(UUID setId, SetType type, int reps, BigDecimal weight, Integer rir);

    void deleteSet(UUID setId);

    List<PreviousSession> previousSessions(UUID userId, UUID exerciseId, int limit);

    List<StaleSession> staleActive(Instant threshold);

    void autoClose(UUID sessionId, Instant finishedAt, int durationSeconds);

    record ExerciseInfo(UUID id, String name, boolean bodyweight, boolean custom, UUID ownerId) {}
}
