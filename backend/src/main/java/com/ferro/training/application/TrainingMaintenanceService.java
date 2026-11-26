package com.ferro.training.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.platform.api.EmailNotifier;
import com.ferro.training.application.port.TrainingRepository;
import com.ferro.training.domain.StaleSession;

/** RN-09: cierra sesiones ACTIVE con más de 24 h. */
@Service
public class TrainingMaintenanceService {

    private static final Duration MAX_DURATION = Duration.ofHours(24);

    private final TrainingRepository sessions;
    private final EmailNotifier emailNotifier;
    private final Clock clock;

    public TrainingMaintenanceService(TrainingRepository sessions, EmailNotifier emailNotifier, Clock clock) {
        this.sessions = sessions;
        this.emailNotifier = emailNotifier;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public int autoCloseStaleSessions() {
        Instant now = clock.instant();
        List<StaleSession> stale = sessions.staleActive(now.minus(MAX_DURATION));
        for (StaleSession session : stale) {
            Instant lastActivity = session.lastSetAt() == null ? session.startedAt() : session.lastSetAt();
            Instant cap = session.startedAt().plus(MAX_DURATION);
            Instant end = lastActivity.isAfter(cap) ? cap : lastActivity;
            int duration =
                    (int) Math.max(0, Duration.between(session.startedAt(), end).toSeconds());
            sessions.autoClose(session.sessionId(), end, duration);
            emailNotifier.sendSessionAutoClosed(session.userEmail());
        }
        return stale.size();
    }
}
