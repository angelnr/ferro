package com.ferro.training.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.shared.domain.UuidV7;
import com.ferro.training.application.port.RoutineRepository;
import com.ferro.training.application.port.TrainingRepository;
import com.ferro.training.domain.InvalidSessionStateException;
import com.ferro.training.domain.InvalidTrainingException;
import com.ferro.training.domain.NewPersonalRecord;
import com.ferro.training.domain.PreviousSession;
import com.ferro.training.domain.Routine;
import com.ferro.training.domain.RoutineExercise;
import com.ferro.training.domain.RoutineNotFoundException;
import com.ferro.training.domain.RoutineSet;
import com.ferro.training.domain.SessionExercise;
import com.ferro.training.domain.SessionNotFoundException;
import com.ferro.training.domain.SessionSet;
import com.ferro.training.domain.SessionStatus;
import com.ferro.training.domain.SetMath;
import com.ferro.training.domain.SetType;
import com.ferro.training.domain.WorkoutSession;

/** M3: sesión en vivo, series inmediatas y cierre con volumen/PRs. */
@Service
public class TrainingService {

    private static final int MAX_EXERCISES = 30;
    private static final int MAX_SETS = 15;

    private final TrainingRepository sessions;
    private final RoutineRepository routines;
    private final Clock clock;

    public TrainingService(TrainingRepository sessions, RoutineRepository routines, Clock clock) {
        this.sessions = sessions;
        this.routines = routines;
        this.clock = clock;
    }

    @Transactional
    public WorkoutSession start(UUID userId) {
        Instant now = clock.instant();
        String name = "Sesión del "
                + DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        .withZone(ZoneId.of("Europe/Madrid"))
                        .format(now);
        return sessions.create(userId, UuidV7.generate(), name, null, now);
    }

    @Transactional
    public WorkoutSession startFromRoutine(UUID userId, UUID routineId) {
        Routine routine = routines.find(userId, routineId).orElseThrow(RoutineNotFoundException::new);
        Instant now = clock.instant();
        WorkoutSession session = sessions.create(userId, UuidV7.generate(), routine.name(), routine.id(), now);
        for (RoutineExercise exercise : routine.exercises()) {
            UUID entryId = UuidV7.generate();
            sessions.addExercise(
                    session.id(),
                    entryId,
                    exercise.exerciseId(),
                    exercise.position(),
                    exercise.restSeconds(),
                    exercise.supersetGroup(),
                    exercise.notes());
            for (RoutineSet target : exercise.sets()) {
                sessions.addTargetSet(
                        entryId,
                        UuidV7.generate(),
                        target.setType(),
                        target.targetReps(),
                        target.targetWeight(),
                        target.targetRir(),
                        target.position());
            }
        }
        return get(userId, session.id());
    }

    @Transactional(readOnly = true)
    public java.util.Optional<WorkoutSession> active(UUID userId) {
        return sessions.findActive(userId);
    }

    @Transactional(readOnly = true)
    public WorkoutSession get(UUID userId, UUID sessionId) {
        return sessions.find(userId, sessionId).orElseThrow(SessionNotFoundException::new);
    }

    @Transactional
    public WorkoutSession addExercise(UUID userId, UUID sessionId, UUID exerciseId) {
        WorkoutSession session = activeFor(userId, sessionId);
        if (sessions.exerciseCount(sessionId) >= MAX_EXERCISES) {
            throw new InvalidTrainingException("Una sesión no puede tener más de 30 ejercicios");
        }
        TrainingRepository.ExerciseInfo exercise =
                sessions.exercise(exerciseId).orElseThrow(SessionNotFoundException::new);
        if (exercise.custom() && !userId.equals(exercise.ownerId())) {
            throw new SessionNotFoundException();
        }
        sessions.addExercise(
                sessionId, UuidV7.generate(), exerciseId, session.exercises().size(), null, null, null);
        return get(userId, sessionId);
    }

    @Transactional
    public SessionSet logSet(
            UUID userId,
            UUID sessionId,
            UUID entryId,
            SetType type,
            int reps,
            BigDecimal weight,
            Integer rir,
            String notes,
            UUID targetSetId) {
        WorkoutSession session = activeFor(userId, sessionId);
        SessionExercise entry = session.exercises().stream()
                .filter(exercise -> exercise.id().equals(entryId))
                .findFirst()
                .orElseThrow(SessionNotFoundException::new);
        if (entry.sets().size() >= MAX_SETS) {
            throw new InvalidTrainingException("Un ejercicio no puede tener más de 15 series");
        }
        if (targetSetId != null
                && entry.targetSets().stream().noneMatch(target -> target.id().equals(targetSetId))) {
            throw new SessionNotFoundException();
        }
        if (reps < 1 || reps > 1000) {
            throw new InvalidTrainingException("Las reps deben estar entre 1 y 1000");
        }
        if (weight == null
                || weight.compareTo(BigDecimal.valueOf(-1000)) < 0
                || weight.compareTo(BigDecimal.valueOf(1000)) > 0) {
            throw new InvalidTrainingException("El peso debe estar entre -1000 y 1000 kg");
        }
        if (rir != null && (rir < 0 || rir > 10)) {
            throw new InvalidTrainingException("El RIR debe estar entre 0 y 10");
        }
        if (notes != null && notes.length() > 500) {
            throw new InvalidTrainingException("La nota no puede superar 500 caracteres");
        }
        Instant now = clock.instant();
        SessionSet created = sessions.addSet(
                entryId,
                UuidV7.generate(),
                type,
                reps,
                weight,
                rir,
                SetMath.volume(type, weight, reps),
                SetMath.e1rm(type, weight, reps),
                now,
                notes);
        if (targetSetId != null) {
            sessions.deleteTargetSet(targetSetId);
        }
        return created;
    }

    @Transactional
    public WorkoutSession updateSession(UUID userId, UUID sessionId, String name, String notes) {
        WorkoutSession session = get(userId, sessionId);
        if (name != null && name.length() > 80) {
            throw new InvalidTrainingException("El nombre no puede superar 80 caracteres");
        }
        if (notes != null && notes.length() > 500) {
            throw new InvalidTrainingException("Las notas no pueden superar 500 caracteres");
        }
        sessions.updateSession(session.id(), name, notes);
        return get(userId, sessionId);
    }

    @Transactional
    public WorkoutSession updateExercise(
            UUID userId,
            UUID sessionId,
            UUID entryId,
            Integer position,
            Integer restSeconds,
            Integer supersetGroup,
            String notes) {
        activeFor(userId, sessionId);
        WorkoutSession session = get(userId, sessionId);
        SessionExercise entry = session.exercises().stream()
                .filter(exercise -> exercise.id().equals(entryId))
                .findFirst()
                .orElseThrow(SessionNotFoundException::new);
        if (restSeconds != null && (restSeconds < 15 || restSeconds > 600)) {
            throw new InvalidTrainingException("El descanso debe estar entre 15 y 600 segundos");
        }
        if (notes != null && notes.length() > 500) {
            throw new InvalidTrainingException("Las notas no pueden superar 500 caracteres");
        }
        sessions.updateExercise(entryId, restSeconds, supersetGroup, notes);
        if (position != null) {
            if (position < 0 || position >= session.exercises().size()) {
                throw new InvalidTrainingException("Posición fuera de rango");
            }
            List<UUID> order = new ArrayList<>(
                    session.exercises().stream().map(SessionExercise::id).toList());
            order.remove(entry.id());
            order.add(position, entry.id());
            sessions.reorderExercises(sessionId, order);
        }
        return get(userId, sessionId);
    }

    @Transactional
    public WorkoutSession deleteExercise(UUID userId, UUID sessionId, UUID entryId) {
        activeFor(userId, sessionId);
        sessions.deleteExercise(entryId);
        return get(userId, sessionId);
    }

    @Transactional
    public WorkoutSession groupSuperset(UUID userId, UUID sessionId, List<UUID> entryIds, Integer group) {
        WorkoutSession session = activeFor(userId, sessionId);
        List<UUID> distinct = entryIds.stream().distinct().toList();
        if (group != null) {
            if (distinct.size() < 2 || distinct.size() > 5) {
                throw new InvalidTrainingException("Un superset debe agrupar entre 2 y 5 ejercicios");
            }
            if (group < 1) {
                throw new InvalidTrainingException("Grupo de superset no válido");
            }
            List<Integer> positions = session.exercises().stream()
                    .filter(entry -> distinct.contains(entry.id()))
                    .map(SessionExercise::position)
                    .sorted()
                    .toList();
            if (positions.size() != distinct.size()) {
                throw new SessionNotFoundException();
            }
            for (int i = 1; i < positions.size(); i++) {
                if (positions.get(i) != positions.get(i - 1) + 1) {
                    throw new InvalidTrainingException("El superset debe agrupar ejercicios adyacentes");
                }
            }
        }
        sessions.setSupersetGroup(sessionId, distinct, group);
        return get(userId, sessionId);
    }

    @Transactional
    public WorkoutSession updateSet(
            UUID userId,
            UUID sessionId,
            UUID entryId,
            UUID setId,
            SetType type,
            int reps,
            BigDecimal weight,
            Integer rir,
            String notes) {
        WorkoutSession session = editableFor(userId, sessionId);
        SessionExercise entry = session.exercises().stream()
                .filter(exercise -> exercise.id().equals(entryId))
                .findFirst()
                .orElseThrow(SessionNotFoundException::new);
        entry.sets().stream()
                .filter(set -> set.id().equals(setId))
                .findFirst()
                .orElseThrow(SessionNotFoundException::new);
        validateSet(reps, weight, rir);
        if (notes != null && notes.length() > 500) {
            throw new InvalidTrainingException("La nota no puede superar 500 caracteres");
        }
        sessions.updateSet(setId, type, reps, weight, rir, notes);
        recalculateAfterEdit(userId, session, entry.exerciseId());
        return get(userId, sessionId);
    }

    @Transactional
    public WorkoutSession deleteSet(UUID userId, UUID sessionId, UUID entryId, UUID setId) {
        WorkoutSession session = editableFor(userId, sessionId);
        SessionExercise entry = session.exercises().stream()
                .filter(exercise -> exercise.id().equals(entryId))
                .findFirst()
                .orElseThrow(SessionNotFoundException::new);
        entry.sets().stream()
                .filter(set -> set.id().equals(setId))
                .findFirst()
                .orElseThrow(SessionNotFoundException::new);
        sessions.deleteSet(setId);
        recalculateAfterEdit(userId, session, entry.exerciseId());
        return get(userId, sessionId);
    }

    @Transactional(readOnly = true)
    public List<PreviousSession> previous(UUID userId, UUID exerciseId, int limit) {
        TrainingRepository.ExerciseInfo exercise =
                sessions.exercise(exerciseId).orElseThrow(SessionNotFoundException::new);
        if (exercise.custom() && !userId.equals(exercise.ownerId())) {
            throw new SessionNotFoundException();
        }
        return sessions.previousSessions(userId, exerciseId, limit);
    }

    @Transactional
    public FinishResult finish(UUID userId, UUID sessionId) {
        WorkoutSession session = activeFor(userId, sessionId);
        Instant now = clock.instant();
        int duration =
                (int) Math.max(0, Duration.between(session.startedAt(), now).toSeconds());
        BigDecimal volume = sessions.totalVolume(sessionId);
        sessions.finish(sessionId, now, duration, volume);
        List<NewPersonalRecord> records = sessions.recalculatePersonalRecords(userId, sessionId, now);
        return new FinishResult(get(userId, sessionId), records);
    }

    @Transactional
    public void discard(UUID userId, UUID sessionId) {
        activeFor(userId, sessionId);
        sessions.discard(sessionId, clock.instant());
    }

    @Transactional
    public void deleteSession(UUID userId, UUID sessionId) {
        WorkoutSession session = get(userId, sessionId);
        if (session.status() == SessionStatus.DISCARDED) {
            return;
        }
        List<UUID> exerciseIds = session.exercises().stream()
                .map(SessionExercise::exerciseId)
                .distinct()
                .toList();
        sessions.discard(sessionId, clock.instant());
        if (session.status() != SessionStatus.ACTIVE) {
            exerciseIds.forEach(exerciseId -> sessions.rebuildPersonalRecords(userId, exerciseId));
        }
    }

    private void validateSet(int reps, BigDecimal weight, Integer rir) {
        if (reps < 1 || reps > 1000) {
            throw new InvalidTrainingException("Las reps deben estar entre 1 y 1000");
        }
        if (weight == null
                || weight.compareTo(BigDecimal.valueOf(-1000)) < 0
                || weight.compareTo(BigDecimal.valueOf(1000)) > 0) {
            throw new InvalidTrainingException("El peso debe estar entre -1000 y 1000 kg");
        }
        if (rir != null && (rir < 0 || rir > 10)) {
            throw new InvalidTrainingException("El RIR debe estar entre 0 y 10");
        }
    }

    private WorkoutSession editableFor(UUID userId, UUID sessionId) {
        WorkoutSession session = get(userId, sessionId);
        if (session.status() != SessionStatus.ACTIVE
                && session.status() != SessionStatus.FINISHED
                && session.status() != SessionStatus.AUTO_CLOSED) {
            throw new InvalidSessionStateException("La sesión no se puede editar");
        }
        return session;
    }

    private void recalculateAfterEdit(UUID userId, WorkoutSession session, UUID exerciseId) {
        if (session.status() != SessionStatus.ACTIVE) {
            sessions.updateTotalVolume(session.id(), sessions.totalVolume(session.id()));
            sessions.rebuildPersonalRecords(userId, exerciseId);
        }
    }

    private WorkoutSession activeFor(UUID userId, UUID sessionId) {
        WorkoutSession session = get(userId, sessionId);
        if (session.status() != SessionStatus.ACTIVE) {
            throw new InvalidSessionStateException("La sesión ya no está activa");
        }
        return session;
    }

    public record FinishResult(WorkoutSession session, List<NewPersonalRecord> newPersonalRecords) {}
}
