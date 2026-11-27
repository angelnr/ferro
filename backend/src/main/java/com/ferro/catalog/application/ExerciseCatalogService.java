package com.ferro.catalog.application;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.catalog.application.port.ExerciseRepository;
import com.ferro.catalog.application.port.ExerciseStatsRepository;
import com.ferro.catalog.domain.Equipment;
import com.ferro.catalog.domain.Exercise;
import com.ferro.catalog.domain.ExerciseDetail;
import com.ferro.catalog.domain.ExerciseInUseException;
import com.ferro.catalog.domain.ExerciseLimitReachedException;
import com.ferro.catalog.domain.ExerciseNotFoundException;
import com.ferro.catalog.domain.InvalidExerciseException;
import com.ferro.catalog.domain.Mechanics;
import com.ferro.catalog.domain.MuscleGroup;
import com.ferro.shared.domain.UuidV7;

/** RF-022/023/024: CRUD de ejercicios custom y detalle. */
@Service
public class ExerciseCatalogService {

    private final ExerciseRepository exercises;
    private final ExerciseStatsRepository stats;
    private final CatalogQueryCache cache;
    private final int customLimit;

    public ExerciseCatalogService(
            ExerciseRepository exercises,
            ExerciseStatsRepository stats,
            CatalogQueryCache cache,
            @Value("${ferro.catalog.custom-limit:100}") int customLimit) {
        this.exercises = exercises;
        this.stats = stats;
        this.cache = cache;
        this.customLimit = customLimit;
    }

    @Transactional
    public Exercise create(UUID userId, CreateExerciseCommand command) {
        if (exercises.countCustomByOwner(userId) >= customLimit) {
            throw new ExerciseLimitReachedException();
        }
        String[] names = names(command.nameEs(), command.nameEn());
        Exercise exercise = new Exercise(
                UuidV7.generate(),
                names[0],
                names[1],
                parseMuscle(command.muscleGroup()),
                parseSecondary(command.secondaryMuscles()),
                parseEquipment(command.equipment()),
                parseMechanics(command.mechanics()),
                Boolean.TRUE.equals(command.bodyweight()),
                true,
                userId,
                false);
        Exercise saved = exercises.save(exercise);
        cache.invalidate();
        return saved;
    }

    @Transactional
    public Exercise update(UUID userId, UUID exerciseId, UpdateExerciseCommand command) {
        Exercise current = exercises.findById(exerciseId).orElseThrow(ExerciseNotFoundException::new);
        if (!current.custom() || !userId.equals(current.ownerId())) {
            throw new ExerciseNotFoundException();
        }
        boolean changesContent = command.nameEs() != null
                || command.nameEn() != null
                || command.muscleGroup() != null
                || command.secondaryMuscles() != null
                || command.equipment() != null
                || command.mechanics() != null
                || command.bodyweight() != null;
        if (changesContent && exercises.hasHistory(exerciseId)) {
            throw new ExerciseInUseException();
        }

        String[] names = command.nameEs() == null && command.nameEn() == null
                ? new String[] {current.nameEs(), current.nameEn()}
                : names(
                        command.nameEs() == null ? current.nameEs() : command.nameEs(),
                        command.nameEn() == null ? current.nameEn() : command.nameEn());
        Exercise updated = new Exercise(
                current.id(),
                names[0],
                names[1],
                command.muscleGroup() == null ? current.muscleGroup() : parseMuscle(command.muscleGroup()),
                command.secondaryMuscles() == null
                        ? current.secondaryMuscles()
                        : parseSecondary(command.secondaryMuscles()),
                command.equipment() == null ? current.equipment() : parseEquipment(command.equipment()),
                command.mechanics() == null ? current.mechanics() : parseMechanics(command.mechanics()),
                command.bodyweight() == null ? current.bodyweight() : command.bodyweight(),
                current.custom(),
                current.ownerId(),
                command.archived() == null ? current.archived() : command.archived());
        Exercise saved = exercises.save(updated);
        cache.invalidate();
        return saved;
    }

    @Transactional(readOnly = true)
    public ExerciseDetail detail(UUID userId, UUID exerciseId) {
        Exercise exercise = exercises.findById(exerciseId).orElseThrow(ExerciseNotFoundException::new);
        if (exercise.custom() && !userId.equals(exercise.ownerId())) {
            throw new ExerciseNotFoundException();
        }
        return new ExerciseDetail(exercise, stats.statsFor(userId, exerciseId));
    }

    private String[] names(String nameEs, String nameEn) {
        String es = nameEs == null ? "" : nameEs.trim();
        String en = nameEn == null ? "" : nameEn.trim();
        if (es.isEmpty() && en.isEmpty()) {
            throw new InvalidExerciseException("El ejercicio necesita al menos un nombre");
        }
        if (es.isEmpty()) {
            es = en;
        }
        if (en.isEmpty()) {
            en = es;
        }
        if (es.length() > 120 || en.length() > 120) {
            throw new InvalidExerciseException("El nombre no puede superar 120 caracteres");
        }
        return new String[] {es, en};
    }

    private MuscleGroup parseMuscle(String value) {
        try {
            return MuscleGroup.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new InvalidExerciseException("Grupo muscular no válido: " + value);
        }
    }

    private Equipment parseEquipment(String value) {
        try {
            return Equipment.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new InvalidExerciseException("Equipamiento no válido: " + value);
        }
    }

    private Mechanics parseMechanics(String value) {
        try {
            return Mechanics.valueOf(value.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new InvalidExerciseException("Mecánica no válida: " + value);
        }
    }

    private List<MuscleGroup> parseSecondary(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().map(this::parseMuscle).toList();
    }
}
