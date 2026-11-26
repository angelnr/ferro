package com.ferro.body.application.port;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.ferro.body.domain.BodyMeasurement;
import com.ferro.body.domain.BodyPage;

public interface BodyRepository {

    BodyPage findAll(UUID userId, Instant from, Instant to, int page, int size);

    Optional<BodyMeasurement> find(UUID userId, UUID id);

    Optional<BodyMeasurement> findByDay(UUID userId, Instant measuredAt, String zone);

    String timeZone(UUID userId);

    void create(UUID userId, BodyMeasurement measurement);

    void update(UUID userId, BodyMeasurement measurement);

    void delete(UUID userId, UUID id);

    java.util.List<BodyMeasurement> since(UUID userId, Instant from);
}
