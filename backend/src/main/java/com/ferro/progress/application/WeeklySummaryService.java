package com.ferro.progress.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.platform.api.EmailNotifier;
import com.ferro.progress.application.port.ProgressRepository;
import com.ferro.progress.domain.ProgressSummary;

/** RF-066: resumen semanal opt-in, lunes a las 08:00 en la zona del usuario. */
@Service
public class WeeklySummaryService {

    private final ProgressRepository progress;
    private final EmailNotifier emailNotifier;
    private final Clock clock;

    public WeeklySummaryService(ProgressRepository progress, EmailNotifier emailNotifier, Clock clock) {
        this.progress = progress;
        this.emailNotifier = emailNotifier;
        this.clock = clock;
    }

    @Scheduled(cron = "0 5 * * * *")
    @Transactional(readOnly = true)
    public void scheduled() {
        sendDue(clock.instant());
    }

    public int sendDue(Instant now) {
        if (!progress.weeklySummaryEnabled()) {
            return 0;
        }
        int sent = 0;
        for (ProgressRepository.WeeklyRecipient recipient : progress.weeklyRecipients()) {
            ZoneId zone = ZoneId.of(recipient.zone());
            ZonedDateTime local = now.atZone(zone);
            if (local.getDayOfWeek() != DayOfWeek.MONDAY || local.getHour() != 8) {
                continue;
            }
            LocalDate thisMonday = local.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            Instant from = thisMonday.minusWeeks(1).atStartOfDay(zone).toInstant();
            Instant to = thisMonday.atStartOfDay(zone).toInstant();
            ProgressSummary.PeriodTotals totals = progress.totals(recipient.userId(), from, to);
            int prs = progress.personalRecordsBetween(recipient.userId(), from, to);
            int streak = currentStreak(recipient.userId(), recipient.zone(), thisMonday.minusDays(1));
            emailNotifier.sendWeeklySummary(
                    recipient.email(), recipient.locale(), (int) totals.sessions(), totals.volume(), prs, streak);
            sent++;
        }
        return sent;
    }

    private int currentStreak(java.util.UUID userId, String zone, LocalDate today) {
        java.util.List<LocalDate> dates = progress.finishedDates(userId, zone);
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
