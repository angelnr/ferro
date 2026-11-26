package com.ferro.training.interfaces.rest;

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
import org.springframework.web.bind.annotation.RestController;

import com.ferro.shared.domain.InvalidPrincipalException;
import com.ferro.training.application.TrainingService;
import com.ferro.training.domain.InvalidTrainingException;
import com.ferro.training.domain.SessionSet;
import com.ferro.training.domain.SetType;
import com.ferro.training.domain.WorkoutSession;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/workout-sessions")
@Tag(name = "Training")
@SecurityRequirement(name = "bearerAuth")
public class TrainingController {

    private final TrainingService training;

    public TrainingController(TrainingService training) {
        this.training = training;
    }

    @PostMapping
    @Operation(summary = "Crea una sesión vacía")
    public ResponseEntity<WorkoutSessionResponse> start(@AuthenticationPrincipal Jwt jwt) {
        WorkoutSession session = training.start(userId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkoutSessionResponse.from(session));
    }

    @GetMapping("/active")
    @Operation(summary = "Devuelve la sesión activa, si existe")
    public ResponseEntity<WorkoutSessionResponse> active(@AuthenticationPrincipal Jwt jwt) {
        return training.active(userId(jwt))
                .map(session -> ResponseEntity.ok(WorkoutSessionResponse.from(session)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una sesión")
    public WorkoutSessionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return WorkoutSessionResponse.from(training.get(userId(jwt), id));
    }

    @PostMapping("/{id}/exercises")
    @Operation(summary = "Añade un ejercicio a la sesión")
    public ResponseEntity<WorkoutSessionResponse> addExercise(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody AddExerciseRequest request) {
        WorkoutSession session = training.addExercise(userId(jwt), id, request.exerciseId());
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkoutSessionResponse.from(session));
    }

    @PostMapping("/{id}/exercises/{entryId}/sets")
    @Operation(summary = "Registra una serie de forma inmediata")
    public ResponseEntity<WorkoutSessionResponse.SetEntry> logSet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID entryId,
            @Valid @RequestBody LogSetRequest request) {
        SetType type = parseSetType(request.setType());
        SessionSet set =
                training.logSet(userId(jwt), id, entryId, type, request.reps(), request.weight(), request.rir());
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkoutSessionResponse.SetEntry.from(set));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edita nombre o notas de la sesión")
    public WorkoutSessionResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateSessionRequest request) {
        return WorkoutSessionResponse.from(training.updateSession(userId(jwt), id, request.name(), request.notes()));
    }

    @PatchMapping("/{id}/exercises/{entryId}")
    @Operation(summary = "Edita o reordena un ejercicio de la sesión")
    public WorkoutSessionResponse updateExercise(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID entryId,
            @Valid @RequestBody UpdateExerciseRequest request) {
        return WorkoutSessionResponse.from(training.updateExercise(
                userId(jwt),
                id,
                entryId,
                request.position(),
                request.restSeconds(),
                request.supersetGroup(),
                request.notes()));
    }

    @DeleteMapping("/{id}/exercises/{entryId}")
    @Operation(summary = "Elimina un ejercicio y sus series")
    public WorkoutSessionResponse deleteExercise(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID entryId) {
        return WorkoutSessionResponse.from(training.deleteExercise(userId(jwt), id, entryId));
    }

    @PatchMapping("/{id}/exercises/{entryId}/sets/{setId}")
    @Operation(summary = "Edita una serie")
    public WorkoutSessionResponse updateSet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID entryId,
            @PathVariable UUID setId,
            @Valid @RequestBody UpdateSetRequest request) {
        SetType type = request.setType() == null ? SetType.NORMAL : parseSetType(request.setType());
        return WorkoutSessionResponse.from(training.updateSet(
                userId(jwt),
                id,
                entryId,
                setId,
                type,
                request.reps() == null ? 0 : request.reps(),
                request.weight() == null ? java.math.BigDecimal.ZERO : request.weight(),
                request.rir()));
    }

    @DeleteMapping("/{id}/exercises/{entryId}/sets/{setId}")
    @Operation(summary = "Elimina una serie")
    public WorkoutSessionResponse deleteSet(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID entryId,
            @PathVariable UUID setId) {
        return WorkoutSessionResponse.from(training.deleteSet(userId(jwt), id, entryId, setId));
    }

    @PostMapping("/{id}/finish")
    @Operation(summary = "Finaliza la sesión y calcula PRs")
    public FinishSessionResponse finish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return FinishSessionResponse.from(training.finish(userId(jwt), id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Descarta la sesión activa")
    public ResponseEntity<Void> discard(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        training.discard(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    private SetType parseSetType(String value) {
        if (value == null || value.isBlank()) {
            return SetType.NORMAL;
        }
        try {
            return SetType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new InvalidTrainingException("Tipo de serie no válido: " + value);
        }
    }

    private UUID userId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidPrincipalException();
        }
    }
}
