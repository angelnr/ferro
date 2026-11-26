package com.ferro.progress.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PersonalRecordGroup(
        UUID exerciseId, String exerciseName, List<RecordEntry> current, List<RecordEntry> history) {

    public record RecordEntry(String type, BigDecimal value, Instant achievedAt) {}
}
