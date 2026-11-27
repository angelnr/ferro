package com.ferro.progress.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.progress.application.port.ProgressRepository;
import com.ferro.progress.domain.PersonalRecordGroup;
import com.ferro.progress.domain.ProgressNotFoundException;
import com.ferro.progress.domain.ProgressSummary;
import com.ferro.progress.domain.SessionDetail;
import com.ferro.progress.domain.SessionPage;
import com.ferro.progress.domain.VolumePoint;

@Service
public class ProgressService {

    private final ProgressRepository progress;
    private final Clock clock;

    public ProgressService(ProgressRepository progress, Clock clock) {
        this.progress = progress;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public SessionPage sessions(UUID userId, Instant from, Instant to, UUID exerciseId, int page, int size) {
        return progress.findSessions(userId, from, to, exerciseId, Math.max(0, page), Math.max(1, Math.min(size, 100)));
    }

    @Transactional(readOnly = true)
    public SessionDetail session(UUID userId, UUID sessionId) {
        return progress.findSession(userId, sessionId).orElseThrow(ProgressNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public List<PersonalRecordGroup> personalRecords(UUID userId, UUID exerciseId) {
        return progress.personalRecords(userId, exerciseId);
    }

    @Transactional(readOnly = true)
    public ProgressSummary summary(UUID userId, int weeks) {
        int safeWeeks = switch (weeks) {
            case 4, 12, 24 -> weeks;
            default -> 12;
        };
        String zone = progress.timeZone(userId);
        ZoneId zoneId = ZoneId.of(zone);
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zoneId);
        LocalDate currentWeek = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant periodStart =
                currentWeek.minusWeeks(safeWeeks - 1).atStartOfDay(zoneId).toInstant();
        Instant periodEnd = now.plusDays(1).toLocalDate().atStartOfDay(zoneId).toInstant();
        Instant previousStart =
                currentWeek.minusWeeks((safeWeeks * 2) - 1).atStartOfDay(zoneId).toInstant();

        List<LocalDate> dates = progress.finishedDates(userId, zone);
        int currentStreak = currentStreak(dates, now.toLocalDate());
        int bestStreak = bestStreak(dates);

        return new ProgressSummary(
                safeWeeks,
                progress.weeklyVolume(userId, periodStart, periodEnd, zone),
                progress.weeklySessions(userId, periodStart, periodEnd, zone),
                currentStreak,
                bestStreak,
                progress.totals(userId, periodStart, periodEnd),
                progress.totals(userId, previousStart, periodStart),
                progress.muscleDistribution(userId, now.minusWeeks(4).toInstant(), periodEnd),
                periodStart,
                periodEnd);
    }

    @Transactional(readOnly = true)
    public Streak streak(UUID userId) {
        String zone = progress.timeZone(userId);
        java.time.LocalDate today = java.time.ZonedDateTime.now(clock)
                .withZoneSameInstant(java.time.ZoneId.of(zone))
                .toLocalDate();
        List<java.time.LocalDate> dates = progress.finishedDates(userId, zone);
        return new Streak(currentStreak(dates, today), bestStreak(dates));
    }

    @Transactional(readOnly = true)
    public List<VolumePoint> volume(UUID userId, Instant from, Instant to, String granularity) {
        String zone = progress.timeZone(userId);
        Instant end = to == null ? clock.instant() : to;
        Instant start =
                from == null ? end.atZone(ZoneId.of(zone)).minusWeeks(12).toInstant() : from;
        return progress.volume(userId, start, end, zone, granularity == null ? "week" : granularity);
    }

    public record Streak(int current, int best) {}

    private int currentStreak(List<LocalDate> dates, LocalDate today) {
        if (dates.isEmpty()) {
            return 0;
        }
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        if (!dates.contains(cursor)) {
            return 0;
        }
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private int bestStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) {
            return 0;
        }
        List<LocalDate> sorted = new ArrayList<>(dates);
        sorted.sort(Comparator.naturalOrder());
        int best = 1;
        int current = 1;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).equals(sorted.get(i - 1).plusDays(1))) {
                current++;
                best = Math.max(best, current);
            } else if (!sorted.get(i).equals(sorted.get(i - 1))) {
                current = 1;
            }
        }
        return best;
    }
}
