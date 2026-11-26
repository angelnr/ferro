package com.ferro.training.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.shared.domain.UuidV7;
import com.ferro.training.application.port.RoutineRepository;
import com.ferro.training.application.port.TrainingRepository;
import com.ferro.training.domain.InvalidTrainingException;
import com.ferro.training.domain.Routine;
import com.ferro.training.domain.RoutineExercise;
import com.ferro.training.domain.RoutineNotFoundException;
import com.ferro.training.domain.RoutinePage;
import com.ferro.training.domain.RoutineSet;
import com.ferro.training.domain.RoutineSummary;
import com.ferro.training.domain.SetType;

/** M4: plantillas de entrenamiento y sus series objetivo. */
@Service
public class RoutineService {

    private static final int MAX_EXERCISES = 30;
    private static final int MAX_SETS = 15;

    private final RoutineRepository routines;
    private final TrainingRepository training;

    public RoutineService(RoutineRepository routines, TrainingRepository training) {
        this.routines = routines;
        this.training = training;
    }

    @Transactional(readOnly = true)
    public RoutinePage list(UUID userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        long total = routines.count(userId);
        List<RoutineSummary> content = routines.findAll(userId, safePage, safeSize);
        int totalPages = (int) Math.ceil((double) total / safeSize);
        return new RoutinePage(content, safePage, safeSize, total, totalPages);
    }

    @Transactional(readOnly = true)
    public Routine get(UUID userId, UUID routineId) {
        return routines.find(userId, routineId).orElseThrow(RoutineNotFoundException::new);
    }

    @Transactional
    public Routine create(UUID userId, RoutineDraft draft) {
        Routine routine = build(userId, UuidV7.generate(), draft);
        routines.create(userId, routine);
        return get(userId, routine.id());
    }

    @Transactional
    public Routine update(UUID userId, UUID routineId, RoutineDraft draft) {
        get(userId, routineId);
        Routine routine = build(userId, routineId, draft);
        routines.replace(userId, routine);
        return get(userId, routineId);
    }

    @Transactional
    public Routine duplicate(UUID userId, UUID routineId) {
        Routine original = get(userId, routineId);
        String name = original.name() + " (copia)";
        int suffix = 2;
        while (routines.nameExists(userId, name)) {
            name = original.name() + " (copia " + suffix + ")";
            suffix++;
        }
        Routine copy = new Routine(
                UuidV7.generate(),
                name,
                original.notes(),
                original.exercises().stream()
                        .map(exercise -> new RoutineExercise(
                                UuidV7.generate(),
                                exercise.exerciseId(),
                                exercise.exerciseName(),
                                exercise.position(),
                                exercise.restSeconds(),
                                exercise.supersetGroup(),
                                exercise.notes(),
                                exercise.sets().stream()
                                        .map(set -> new RoutineSet(
                                                UuidV7.generate(),
                                                set.position(),
                                                set.setType(),
                                                set.targetReps(),
                                                set.targetWeight(),
                                                set.targetRir()))
                                        .toList()))
                        .toList());
        routines.create(userId, copy);
        return get(userId, copy.id());
    }

    @Transactional
    public void delete(UUID userId, UUID routineId) {
        get(userId, routineId);
        routines.delete(userId, routineId);
    }

    private Routine build(UUID userId, UUID routineId, RoutineDraft draft) {
        String name = draft.name() == null ? "" : draft.name().trim();
        if (name.isBlank()) {
            throw new InvalidTrainingException("El nombre de la rutina es obligatorio");
        }
        if (name.length() > 80) {
            throw new InvalidTrainingException("El nombre no puede superar 80 caracteres");
        }
        String notes = draft.notes();
        if (notes != null && notes.length() > 500) {
            throw new InvalidTrainingException("Las notas no pueden superar 500 caracteres");
        }
        if (draft.exercises() == null || draft.exercises().isEmpty()) {
            throw new InvalidTrainingException("La rutina debe tener al menos un ejercicio");
        }
        if (draft.exercises().size() > MAX_EXERCISES) {
            throw new InvalidTrainingException("Una rutina no puede tener más de 30 ejercicios");
        }
        validateDraftExercises(draft.exercises());
        validateSupersets(draft.exercises());

        List<RoutineExercise> exercises = new ArrayList<>();
        for (int index = 0; index < draft.exercises().size(); index++) {
            RoutineDraft.ExerciseDraft exercise = draft.exercises().get(index);
            TrainingRepository.ExerciseInfo info =
                    training.exercise(exercise.exerciseId()).orElseThrow(RoutineNotFoundException::new);
            if (info.custom() && !userId.equals(info.ownerId())) {
                throw new RoutineNotFoundException();
            }
            List<RoutineSet> sets = new ArrayList<>();
            for (int setIndex = 0; setIndex < exercise.sets().size(); setIndex++) {
                RoutineDraft.SetDraft set = exercise.sets().get(setIndex);
                sets.add(new RoutineSet(
                        UuidV7.generate(),
                        setIndex,
                        set.setType() == null ? SetType.NORMAL : set.setType(),
                        set.targetReps(),
                        set.targetWeight(),
                        set.targetRir()));
            }
            exercises.add(new RoutineExercise(
                    UuidV7.generate(),
                    exercise.exerciseId(),
                    info.name(),
                    index,
                    exercise.restSeconds(),
                    exercise.supersetGroup(),
                    exercise.notes(),
                    sets));
        }
        return new Routine(routineId, name, notes, exercises);
    }

    private void validateDraftExercises(List<RoutineDraft.ExerciseDraft> exercises) {
        for (RoutineDraft.ExerciseDraft exercise : exercises) {
            if (exercise.exerciseId() == null) {
                throw new InvalidTrainingException("Falta el ejercicio de la rutina");
            }
            if (exercise.restSeconds() != null && (exercise.restSeconds() < 15 || exercise.restSeconds() > 600)) {
                throw new InvalidTrainingException("El descanso debe estar entre 15 y 600 segundos");
            }
            if (exercise.notes() != null && exercise.notes().length() > 500) {
                throw new InvalidTrainingException("Las notas no pueden superar 500 caracteres");
            }
            if (exercise.sets() == null) {
                throw new InvalidTrainingException("Las series objetivo son obligatorias");
            }
            if (exercise.sets().size() > MAX_SETS) {
                throw new InvalidTrainingException("Un ejercicio no puede tener más de 15 series objetivo");
            }
            for (RoutineDraft.SetDraft set : exercise.sets()) {
                if (set.targetReps() < 1 || set.targetReps() > 1000) {
                    throw new InvalidTrainingException("Las reps objetivo deben estar entre 1 y 1000");
                }
                if (set.targetWeight() != null
                        && (set.targetWeight().compareTo(BigDecimal.valueOf(-1000)) < 0
                                || set.targetWeight().compareTo(BigDecimal.valueOf(1000)) > 0)) {
                    throw new InvalidTrainingException("El peso objetivo debe estar entre -1000 y 1000 kg");
                }
                if (set.targetRir() != null && (set.targetRir() < 0 || set.targetRir() > 10)) {
                    throw new InvalidTrainingException("El RIR objetivo debe estar entre 0 y 10");
                }
            }
        }
    }

    private void validateSupersets(List<RoutineDraft.ExerciseDraft> exercises) {
        Map<Integer, List<Integer>> groups = new HashMap<>();
        for (int index = 0; index < exercises.size(); index++) {
            Integer group = exercises.get(index).supersetGroup();
            if (group == null) {
                continue;
            }
            if (group < 1) {
                throw new InvalidTrainingException("Grupo de superset no válido");
            }
            groups.computeIfAbsent(group, key -> new ArrayList<>()).add(index);
        }
        for (List<Integer> positions : groups.values()) {
            if (positions.size() < 2 || positions.size() > 5) {
                throw new InvalidTrainingException("Un superset debe agrupar entre 2 y 5 ejercicios");
            }
            for (int index = 1; index < positions.size(); index++) {
                if (positions.get(index) != positions.get(index - 1) + 1) {
                    throw new InvalidTrainingException("El superset debe agrupar ejercicios adyacentes");
                }
            }
        }
    }

    public record RoutineDraft(String name, String notes, List<ExerciseDraft> exercises) {

        public record ExerciseDraft(
                UUID exerciseId, Integer restSeconds, Integer supersetGroup, String notes, List<SetDraft> sets) {}

        public record SetDraft(SetType setType, int targetReps, BigDecimal targetWeight, Integer targetRir) {}
    }
}
