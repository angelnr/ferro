package com.ferro.training.interfaces.rest;

import java.util.List;

import com.ferro.training.application.TrainingService;

public record FinishSessionResponse(
        WorkoutSessionResponse session, List<NewPersonalRecordResponse> newPersonalRecords) {

    public static FinishSessionResponse from(TrainingService.FinishResult result) {
        return new FinishSessionResponse(
                WorkoutSessionResponse.from(result.session()),
                result.newPersonalRecords().stream()
                        .map(NewPersonalRecordResponse::from)
                        .toList());
    }
}
