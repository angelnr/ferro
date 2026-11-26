package com.ferro.catalog.interfaces.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ferro.catalog.application.CreateExerciseCommand;
import com.ferro.catalog.application.ExerciseCatalogService;
import com.ferro.catalog.application.ExerciseSearchService;
import com.ferro.catalog.application.UpdateExerciseCommand;
import com.ferro.catalog.domain.Exercise;
import com.ferro.catalog.domain.PageResult;
import com.ferro.shared.domain.InvalidPrincipalException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/exercises")
@Tag(name = "Catalog")
@SecurityRequirement(name = "bearerAuth")
public class ExerciseController {

    private final ExerciseSearchService searchService;
    private final ExerciseCatalogService catalogService;

    public ExerciseController(ExerciseSearchService searchService, ExerciseCatalogService catalogService) {
        this.searchService = searchService;
        this.catalogService = catalogService;
    }

    @PostMapping
    @Operation(summary = "Crea un ejercicio personalizado")
    public ResponseEntity<ExerciseResponse> create(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateExerciseRequest request) {
        UUID userId = userId(jwt);
        Exercise exercise = catalogService.create(
                userId,
                new CreateExerciseCommand(
                        request.nameEs(),
                        request.nameEn(),
                        request.muscleGroup(),
                        request.secondaryMuscles(),
                        request.equipment(),
                        request.mechanics(),
                        request.bodyweight()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ExerciseResponse.from(exercise, userId));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edita o archiva un ejercicio personalizado")
    public ExerciseResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExerciseRequest request) {
        UUID userId = userId(jwt);
        Exercise exercise = catalogService.update(
                userId,
                id,
                new UpdateExerciseCommand(
                        request.nameEs(),
                        request.nameEn(),
                        request.muscleGroup(),
                        request.secondaryMuscles(),
                        request.equipment(),
                        request.mechanics(),
                        request.bodyweight(),
                        request.archived()));
        return ExerciseResponse.from(exercise, userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle del ejercicio con stats del usuario")
    public ExerciseDetailResponse detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        UUID userId = userId(jwt);
        return ExerciseDetailResponse.from(catalogService.detail(userId, id), userId);
    }

    @GetMapping
    @Operation(summary = "Busca ejercicios globales y custom del usuario")
    public ExercisePageResponse search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String muscle,
            @RequestParam(required = false) String equipment,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userId(jwt);
        PageResult<Exercise> result = searchService.search(userId, q, muscle, equipment, page, size);
        List<ExerciseResponse> content = result.content().stream()
                .map(exercise -> ExerciseResponse.from(exercise, userId))
                .toList();
        return ExercisePageResponse.from(
                new PageResult<>(content, result.page(), result.size(), result.totalElements()));
    }

    private UUID userId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidPrincipalException();
        }
    }
}
