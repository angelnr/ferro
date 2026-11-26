package com.ferro.training.interfaces.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.ferro.training.domain.SessionExercise;
import com.ferro.training.domain.SessionSet;
import com.ferro.training.domain.TargetSet;
import com.ferro.training.domain.WorkoutSession;

public record WorkoutSessionResponse(
        String id,
        String name,
        String notes,
        String status,
        Instant startedAt,
        Instant finishedAt,
        Integer durationSeconds,
        BigDecimal totalVolume,
        List<ExerciseEntry> exercises) {

    public static WorkoutSessionResponse from(WorkoutSession session) {
        return new WorkoutSessionResponse(
                session.id().toString(),
                session.name(),
                session.notes(),
                session.status().name(),
                session.startedAt(),
                session.finishedAt(),
                session.durationSeconds(),
                session.totalVolume(),
                session.exercises().stream().map(ExerciseEntry::from).toList());
    }

    public record ExerciseEntry(
            String id,
            String exerciseId,
            String name,
            int position,
            Integer restSeconds,
            Integer supersetGroup,
            String notes,
            List<SetEntry> sets,
            List<TargetSetEntry> targetSets) {

        public static ExerciseEntry from(SessionExercise exercise) {
            return new ExerciseEntry(
                    exercise.id().toString(),
                    exercise.exerciseId().toString(),
                    exercise.exerciseName(),
                    exercise.position(),
                    exercise.restSeconds(),
                    exercise.supersetGroup(),
                    exercise.notes(),
                    exercise.sets().stream().map(SetEntry::from).toList(),
                    exercise.targetSets().stream().map(TargetSetEntry::from).toList());
        }
    }

    public record TargetSetEntry(
            String id, int position, String setType, int targetReps, BigDecimal targetWeight, Integer targetRir) {

        public static TargetSetEntry from(TargetSet set) {
            return new TargetSetEntry(
                    set.id().toString(),
                    set.position(),
                    set.setType().name(),
                    set.targetReps(),
                    set.targetWeight(),
                    set.targetRir());
        }
    }

    public record SetEntry(
            String id,
            int position,
            String setType,
            int reps,
            BigDecimal weight,
            Integer rir,
            BigDecimal volume,
            BigDecimal e1rm,
            Instant loggedAt,
            String notes) {

        public static SetEntry from(SessionSet set) {
            return new SetEntry(
                    set.id().toString(),
                    set.position(),
                    set.setType().name(),
                    set.reps(),
                    set.weight(),
                    set.rir(),
                    set.volume(),
                    set.e1rm(),
                    set.loggedAt(),
                    set.notes());
        }
    }
}
