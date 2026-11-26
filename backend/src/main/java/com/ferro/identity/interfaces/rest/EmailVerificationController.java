package com.ferro.identity.interfaces.rest;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ferro.identity.application.EmailVerificationService;
import com.ferro.identity.domain.InvalidCredentialsException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/auth/email-verification")
@Tag(name = "Auth")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirma el email con el token de un solo uso")
    @ApiResponse(responseCode = "204", description = "Email verificado")
    public ResponseEntity<Void> confirm(@Valid @RequestBody ConfirmRequest request) {
        emailVerificationService.confirm(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend")
    @Operation(summary = "Reenvía el email de verificación al usuario autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "202", description = "Email encolado para envío")
    public ResponseEntity<Void> resend(@AuthenticationPrincipal Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidCredentialsException();
        }
        emailVerificationService.resend(userId);
        return ResponseEntity.accepted().build();
    }

    public record ConfirmRequest(@NotBlank String token) {}
}
