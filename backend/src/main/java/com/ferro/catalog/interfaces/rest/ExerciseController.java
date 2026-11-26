package com.ferro.catalog.interfaces.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ferro.catalog.application.ExerciseSearchService;
import com.ferro.catalog.domain.Exercise;
import com.ferro.catalog.domain.PageResult;
import com.ferro.shared.domain.InvalidPrincipalException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/exercises")
@Tag(name = "Catalog")
@SecurityRequirement(name = "bearerAuth")
public class ExerciseController {

    private final ExerciseSearchService searchService;

    public ExerciseController(ExerciseSearchService searchService) {
        this.searchService = searchService;
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
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidPrincipalException();
        }
        PageResult<Exercise> result = searchService.search(userId, q, muscle, equipment, page, size);
        List<ExerciseResponse> content = result.content().stream()
                .map(exercise -> ExerciseResponse.from(exercise, userId))
                .toList();
        return ExercisePageResponse.from(
                new PageResult<>(content, result.page(), result.size(), result.totalElements()));
    }
}
