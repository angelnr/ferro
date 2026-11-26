package com.ferro.body.interfaces.rest;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ferro.body.application.BodyService;
import com.ferro.body.domain.BodyMeasurement;
import com.ferro.body.domain.BodyPage;
import com.ferro.body.domain.BodyStats;
import com.ferro.shared.domain.InvalidPrincipalException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/body-measurements")
@Tag(name = "Body")
@SecurityRequirement(name = "bearerAuth")
public class BodyController {

    private final BodyService body;

    public BodyController(BodyService body) {
        this.body = body;
    }

    @GetMapping
    @Operation(summary = "Historial de mediciones corporales")
    public BodyPage list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return body.list(userId(jwt), from, to, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una medición corporal")
    public BodyMeasurement get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return body.get(userId(jwt), id);
    }

    @PostMapping
    @Operation(summary = "Registra una medición corporal")
    public ResponseEntity<BodyMeasurement> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BodyMeasurementRequest request) {
        BodyMeasurement created = body.create(userId(jwt), toDomain(null, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edita una medición corporal")
    public BodyMeasurement update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody BodyMeasurementRequest request) {
        return body.update(userId(jwt), id, toDomain(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una medición corporal")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        body.delete(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @Operation(summary = "Series de evolución corporal")
    public BodyStats stats(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "3") int months) {
        return body.stats(userId(jwt), months);
    }

    private BodyMeasurement toDomain(UUID id, BodyMeasurementRequest request) {
        return new BodyMeasurement(
                id,
                request.measuredAt(),
                request.weight(),
                request.chest(),
                request.waist(),
                request.hip(),
                request.arm(),
                request.thigh(),
                request.notes());
    }

    private UUID userId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidPrincipalException();
        }
    }
}
