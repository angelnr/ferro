package com.ferro.training.interfaces.rest;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ferro.shared.domain.InvalidPrincipalException;
import com.ferro.training.application.RoutineService;
import com.ferro.training.application.TrainingService;
import com.ferro.training.domain.InvalidTrainingException;
import com.ferro.training.domain.Routine;
import com.ferro.training.domain.SetType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/routines")
@Tag(name = "Routines")
@SecurityRequirement(name = "bearerAuth")
public class RoutineController {

    private final RoutineService routines;
    private final TrainingService training;

    public RoutineController(RoutineService routines, TrainingService training) {
        this.routines = routines;
        this.training = training;
    }

    @GetMapping
    @Operation(summary = "Lista las rutinas del usuario")
    public RoutineResponse.Page list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return RoutineResponse.Page.from(routines.list(userId(jwt), page, size));
    }

    @PostMapping
    @Operation(summary = "Crea una rutina")
    public ResponseEntity<RoutineResponse> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RoutineRequest request) {
        Routine routine = routines.create(userId(jwt), draft(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(RoutineResponse.from(routine));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una rutina")
    public RoutineResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return RoutineResponse.from(routines.get(userId(jwt), id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Reemplaza el contenido de una rutina")
    public RoutineResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody RoutineRequest request) {
        return RoutineResponse.from(routines.update(userId(jwt), id, draft(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina una rutina")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        routines.delete(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Duplica una rutina")
    public ResponseEntity<RoutineResponse> duplicate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Routine routine = routines.duplicate(userId(jwt), id);
        return ResponseEntity.status(HttpStatus.CREATED).body(RoutineResponse.from(routine));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Inicia una sesión desde una rutina")
    public ResponseEntity<WorkoutSessionResponse> start(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(WorkoutSessionResponse.from(training.startFromRoutine(userId(jwt), id)));
    }

    private RoutineService.RoutineDraft draft(RoutineRequest request) {
        return new RoutineService.RoutineDraft(
                request.name(),
                request.notes(),
                request.exercises().stream()
                        .map(exercise -> new RoutineService.RoutineDraft.ExerciseDraft(
                                exercise.exerciseId(),
                                exercise.restSeconds(),
                                exercise.supersetGroup(),
                                exercise.notes(),
                                exercise.sets().stream()
                                        .map(set -> new RoutineService.RoutineDraft.SetDraft(
                                                parseSetType(set.setType()),
                                                set.targetReps(),
                                                set.targetWeight(),
                                                set.targetRir()))
                                        .toList()))
                        .toList());
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
