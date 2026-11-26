package com.ferro.training.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.shared.domain.UuidV7;
import com.ferro.training.application.port.TrainingRepository;
import com.ferro.training.domain.InvalidSessionStateException;
import com.ferro.training.domain.InvalidTrainingException;
import com.ferro.training.domain.NewPersonalRecord;
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
    private final Clock clock;

    public TrainingService(TrainingRepository sessions, Clock clock) {
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional
    public WorkoutSession start(UUID userId) {
        Instant now = clock.instant();
        String name = "Sesión del "
                + DateTimeFormatter.ofPattern("yyyy-MM-dd")
                        .withZone(ZoneId.of("Europe/Madrid"))
                        .format(now);
        return sessions.create(userId, UuidV7.generate(), name, now);
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
                sessionId, UuidV7.generate(), exerciseId, session.exercises().size());
        return get(userId, sessionId);
    }

    @Transactional
    public SessionSet logSet(
            UUID userId, UUID sessionId, UUID entryId, SetType type, int reps, BigDecimal weight, Integer rir) {
        WorkoutSession session = activeFor(userId, sessionId);
        SessionExercise entry = session.exercises().stream()
                .filter(exercise -> exercise.id().equals(entryId))
                .findFirst()
                .orElseThrow(SessionNotFoundException::new);
        if (entry.sets().size() >= MAX_SETS) {
            throw new InvalidTrainingException("Un ejercicio no puede tener más de 15 series");
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
        Instant now = clock.instant();
        return sessions.addSet(
                entryId,
                UuidV7.generate(),
                type,
                reps,
                weight,
                rir,
                SetMath.volume(type, weight, reps),
                SetMath.e1rm(type, weight, reps),
                now);
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

    private WorkoutSession activeFor(UUID userId, UUID sessionId) {
        WorkoutSession session = get(userId, sessionId);
        if (session.status() != SessionStatus.ACTIVE) {
            throw new InvalidSessionStateException("La sesión ya no está activa");
        }
        return session;
    }

    public record FinishResult(WorkoutSession session, List<NewPersonalRecord> newPersonalRecords) {}
}
