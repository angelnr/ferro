package com.ferro.training.interfaces.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ferro.shared.domain.InvalidPrincipalException;
import com.ferro.training.application.TrainingService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/exercises")
@Tag(name = "Training")
@SecurityRequirement(name = "bearerAuth")
public class ExerciseHistoryController {

    private final TrainingService training;

    public ExerciseHistoryController(TrainingService training) {
        this.training = training;
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Últimas sesiones con las series de un ejercicio")
    public List<PreviousSessionResponse> history(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestParam(defaultValue = "3") int limit) {
        return training.previous(userId(jwt), id, limit).stream()
                .map(PreviousSessionResponse::from)
                .toList();
    }

    private UUID userId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidPrincipalException();
        }
    }
}
