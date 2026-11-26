package com.ferro.training.interfaces.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.ferro.training.domain.Routine;
import com.ferro.training.domain.RoutineExercise;
import com.ferro.training.domain.RoutinePage;
import com.ferro.training.domain.RoutineSet;
import com.ferro.training.domain.RoutineSummary;

public record RoutineResponse(String id, String name, String notes, List<ExerciseEntry> exercises) {

    public static RoutineResponse from(Routine routine) {
        return new RoutineResponse(
                routine.id().toString(),
                routine.name(),
                routine.notes(),
                routine.exercises().stream().map(ExerciseEntry::from).toList());
    }

    public record ExerciseEntry(
            String id,
            String exerciseId,
            String name,
            int position,
            Integer restSeconds,
            Integer supersetGroup,
            String notes,
            List<SetEntry> sets) {

        static ExerciseEntry from(RoutineExercise exercise) {
            return new ExerciseEntry(
                    exercise.id().toString(),
                    exercise.exerciseId().toString(),
                    exercise.exerciseName(),
                    exercise.position(),
                    exercise.restSeconds(),
                    exercise.supersetGroup(),
                    exercise.notes(),
                    exercise.sets().stream().map(SetEntry::from).toList());
        }
    }

    public record SetEntry(
            String id, int position, String setType, int targetReps, BigDecimal targetWeight, Integer targetRir) {

        static SetEntry from(RoutineSet set) {
            return new SetEntry(
                    set.id().toString(),
                    set.position(),
                    set.setType().name(),
                    set.targetReps(),
                    set.targetWeight(),
                    set.targetRir());
        }
    }

    public record Page(List<Summary> content, int page, int size, long totalElements, int totalPages) {

        public static Page from(RoutinePage routines) {
            return new Page(
                    routines.content().stream().map(Summary::from).toList(),
                    routines.page(),
                    routines.size(),
                    routines.totalElements(),
                    routines.totalPages());
        }
    }

    public record Summary(String id, String name, int exerciseCount, Instant lastUsedAt) {

        static Summary from(RoutineSummary routine) {
            return new Summary(routine.id().toString(), routine.name(), routine.exerciseCount(), routine.lastUsedAt());
        }
    }
}
