package com.ferro.progress.application.port;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ferro.progress.domain.PersonalRecordGroup;
import com.ferro.progress.domain.ProgressSummary;
import com.ferro.progress.domain.SessionDetail;
import com.ferro.progress.domain.SessionPage;
import com.ferro.progress.domain.VolumePoint;

public interface ProgressRepository {

    SessionPage findSessions(UUID userId, Instant from, Instant to, UUID exerciseId, int page, int size);

    Optional<SessionDetail> findSession(UUID userId, UUID sessionId);

    List<PersonalRecordGroup> personalRecords(UUID userId, UUID exerciseId);

    String timeZone(UUID userId);

    List<LocalDate> finishedDates(UUID userId, String zone);

    List<ProgressSummary.WeekPoint> weeklyVolume(UUID userId, Instant from, Instant to, String zone);

    List<ProgressSummary.WeekPoint> weeklySessions(UUID userId, Instant from, Instant to, String zone);

    ProgressSummary.PeriodTotals totals(UUID userId, Instant from, Instant to);

    List<ProgressSummary.MuscleSlice> muscleDistribution(UUID userId, Instant from, Instant to);

    int personalRecordsBetween(UUID userId, Instant from, Instant to);

    boolean weeklySummaryEnabled();

    List<WeeklyRecipient> weeklyRecipients();

    List<VolumePoint> volume(UUID userId, Instant from, Instant to, String zone, String granularity);

    record WeeklyRecipient(UUID userId, String email, String locale, String zone) {}
}
