package com.ferro.progress.interfaces.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ferro.progress.application.ProgressService;
import com.ferro.progress.domain.PersonalRecordGroup;
import com.ferro.progress.domain.ProgressSummary;
import com.ferro.progress.domain.SessionDetail;
import com.ferro.progress.domain.SessionPage;
import com.ferro.progress.domain.VolumePoint;
import com.ferro.shared.domain.InvalidPrincipalException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/progress")
@Tag(name = "Progress")
@SecurityRequirement(name = "bearerAuth")
public class ProgressController {

    private final ProgressService progress;

    public ProgressController(ProgressService progress) {
        this.progress = progress;
    }

    @GetMapping("/sessions")
    @Operation(summary = "Historial paginado de sesiones terminadas")
    public SessionPage sessions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) UUID exerciseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return progress.sessions(userId(jwt), from, to, exerciseId, page, size);
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Detalle completo de una sesión terminada")
    public SessionDetail session(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return progress.session(userId(jwt), id);
    }

    @GetMapping("/prs")
    @Operation(summary = "Récords personales activos e histórico")
    public List<PersonalRecordGroup> personalRecords(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID exerciseId) {
        return progress.personalRecords(userId(jwt), exerciseId);
    }

    @GetMapping("/summary")
    @Operation(summary = "Panel de estadísticas por rango de semanas")
    public ProgressSummary summary(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "12") int weeks) {
        return progress.summary(userId(jwt), weeks);
    }

    @GetMapping("/volume")
    @Operation(summary = "Serie de volumen por periodo")
    public List<VolumePoint> volume(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "week") String granularity) {
        return progress.volume(userId(jwt), from, to, granularity);
    }

    private UUID userId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidPrincipalException();
        }
    }
}
