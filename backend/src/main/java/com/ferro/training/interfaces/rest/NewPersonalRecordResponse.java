package com.ferro.training.interfaces.rest;

import java.math.BigDecimal;
import java.time.Instant;

import com.ferro.training.domain.NewPersonalRecord;

public record NewPersonalRecordResponse(
        String exerciseId,
        String exerciseName,
        String prType,
        BigDecimal value,
        BigDecimal previousValue,
        Instant achievedAt) {

    public static NewPersonalRecordResponse from(NewPersonalRecord record) {
        return new NewPersonalRecordResponse(
                record.exerciseId().toString(),
                record.exerciseName(),
                record.prType().name(),
                record.value(),
                record.previousValue(),
                record.achievedAt());
    }
}
