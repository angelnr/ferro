package com.ferro.body.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ferro.body.application.port.BodyRepository;
import com.ferro.body.domain.BodyMeasurement;
import com.ferro.body.domain.BodyNotFoundException;
import com.ferro.body.domain.BodyPage;
import com.ferro.body.domain.BodyStats;
import com.ferro.body.domain.InvalidBodyException;
import com.ferro.body.domain.MeasurementExistsException;
import com.ferro.shared.domain.UuidV7;

@Service
public class BodyService {

    private final BodyRepository measurements;
    private final Clock clock;

    public BodyService(BodyRepository measurements, Clock clock) {
        this.measurements = measurements;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public BodyPage list(UUID userId, Instant from, Instant to, int page, int size) {
        return measurements.findAll(userId, from, to, Math.max(0, page), Math.max(1, Math.min(size, 100)));
    }

    @Transactional(readOnly = true)
    public BodyMeasurement get(UUID userId, UUID id) {
        return measurements.find(userId, id).orElseThrow(BodyNotFoundException::new);
    }

    @Transactional
    public BodyMeasurement create(UUID userId, BodyMeasurement input) {
        validate(input);
        Instant measuredAt = input.measuredAt() == null ? clock.instant() : input.measuredAt();
        String zone = measurements.timeZone(userId);
        measurements.findByDay(userId, measuredAt, zone).ifPresent(existing -> {
            throw new MeasurementExistsException(existing.id());
        });
        BodyMeasurement created = new BodyMeasurement(
                UuidV7.generate(),
                measuredAt,
                input.weight(),
                input.chest(),
                input.waist(),
                input.hip(),
                input.arm(),
                input.thigh(),
                input.notes());
        measurements.create(userId, created);
        return created;
    }

    @Transactional
    public BodyMeasurement update(UUID userId, UUID id, BodyMeasurement input) {
        get(userId, id);
        validate(input);
        BodyMeasurement updated = new BodyMeasurement(
                id,
                input.measuredAt() == null ? clock.instant() : input.measuredAt(),
                input.weight(),
                input.chest(),
                input.waist(),
                input.hip(),
                input.arm(),
                input.thigh(),
                input.notes());
        measurements.update(userId, updated);
        return updated;
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        get(userId, id);
        measurements.delete(userId, id);
    }

    @Transactional(readOnly = true)
    public BodyStats stats(UUID userId, int months) {
        int safeMonths = switch (months) {
            case 1, 3, 6, 12 -> months;
            default -> 3;
        };
        String zone = measurements.timeZone(userId);
        Instant from = ZonedDateTime.now(clock)
                .withZoneSameInstant(ZoneId.of(zone))
                .minusMonths(safeMonths)
                .toInstant();
        List<BodyMeasurement> rows = measurements.since(userId, from);
        List<BodyStats.Point> weight = new ArrayList<>();
        List<BodyStats.Point> chest = new ArrayList<>();
        List<BodyStats.Point> waist = new ArrayList<>();
        List<BodyStats.Point> hip = new ArrayList<>();
        List<BodyStats.Point> arm = new ArrayList<>();
        List<BodyStats.Point> thigh = new ArrayList<>();
        for (BodyMeasurement row : rows) {
            add(weight, row.measuredAt(), row.weight());
            add(chest, row.measuredAt(), row.chest());
            add(waist, row.measuredAt(), row.waist());
            add(hip, row.measuredAt(), row.hip());
            add(arm, row.measuredAt(), row.arm());
            add(thigh, row.measuredAt(), row.thigh());
        }
        return new BodyStats(weight, chest, waist, hip, arm, thigh);
    }

    private void add(List<BodyStats.Point> points, Instant date, BigDecimal value) {
        if (value != null) {
            points.add(new BodyStats.Point(date.truncatedTo(ChronoUnit.SECONDS), value));
        }
    }

    private void validate(BodyMeasurement input) {
        if (input.weight() == null
                && input.chest() == null
                && input.waist() == null
                && input.hip() == null
                && input.arm() == null
                && input.thigh() == null) {
            throw new InvalidBodyException("Registra al menos un peso o perímetro");
        }
        if (input.weight() != null
                && (input.weight().compareTo(BigDecimal.valueOf(20)) < 0
                        || input.weight().compareTo(BigDecimal.valueOf(400)) > 0)) {
            throw new InvalidBodyException("El peso debe estar entre 20 y 400 kg");
        }
        BigDecimal[] perimeters = {input.chest(), input.waist(), input.hip(), input.arm(), input.thigh()};
        for (BigDecimal perimeter : perimeters) {
            if (perimeter != null
                    && (perimeter.compareTo(BigDecimal.valueOf(10)) < 0
                            || perimeter.compareTo(BigDecimal.valueOf(250)) > 0)) {
                throw new InvalidBodyException("Los perímetros deben estar entre 10 y 250 cm");
            }
        }
        if (input.notes() != null && input.notes().length() > 500) {
            throw new InvalidBodyException("Las notas no pueden superar 500 caracteres");
        }
    }
}
