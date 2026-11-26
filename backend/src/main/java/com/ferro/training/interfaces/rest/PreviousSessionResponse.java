package com.ferro.training.interfaces.rest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.ferro.training.domain.PreviousSession;
import com.ferro.training.domain.PreviousSet;

public record PreviousSessionResponse(String sessionId, Instant startedAt, List<PreviousSetResponse> sets) {

    public static PreviousSessionResponse from(PreviousSession session) {
        return new PreviousSessionResponse(
                session.id().toString(),
                session.startedAt(),
                session.sets().stream().map(PreviousSetResponse::from).toList());
    }

    public record PreviousSetResponse(int reps, BigDecimal weight, Integer rir, String setType, Instant loggedAt) {

        static PreviousSetResponse from(PreviousSet set) {
            return new PreviousSetResponse(
                    set.reps(), set.weight(), set.rir(), set.setType().name(), set.loggedAt());
        }
    }
}
