package com.ferro.identity.interfaces.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ferro.identity.application.PasswordResetService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/auth/password-reset")
@Tag(name = "Auth")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/request")
    @Operation(summary = "Solicita un enlace de recuperación (siempre 202)")
    public ResponseEntity<Void> request(@Valid @RequestBody ResetRequest request) {
        passwordResetService.request(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirm")
    @Operation(summary = "Restablece la contraseña con el token de un solo uso")
    public ResponseEntity<Void> confirm(@Valid @RequestBody ResetConfirm request) {
        passwordResetService.confirm(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    public record ResetRequest(@NotBlank @Email String email) {}

    public record ResetConfirm(
            @NotBlank String token,
            @NotBlank @Size(min = 10, max = 128) String newPassword) {}
}
