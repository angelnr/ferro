package com.ferro.identity.interfaces.rest;

import java.time.Duration;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ferro.identity.application.ChangePasswordService;
import com.ferro.identity.application.DeleteAccountService;
import com.ferro.identity.application.RefreshProperties;
import com.ferro.identity.application.UpdateProfileCommand;
import com.ferro.identity.application.UpdateProfileService;
import com.ferro.identity.application.UserDataExportService;
import com.ferro.identity.domain.InvalidCredentialsException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UpdateProfileService updateProfileService;
    private final ChangePasswordService changePasswordService;
    private final DeleteAccountService deleteAccountService;
    private final RefreshProperties refreshProperties;
    private final UserDataExportService dataExportService;

    public UserController(
            UpdateProfileService updateProfileService,
            ChangePasswordService changePasswordService,
            DeleteAccountService deleteAccountService,
            RefreshProperties refreshProperties,
            UserDataExportService dataExportService) {
        this.updateProfileService = updateProfileService;
        this.changePasswordService = changePasswordService;
        this.deleteAccountService = deleteAccountService;
        this.refreshProperties = refreshProperties;
        this.dataExportService = dataExportService;
    }

    @PatchMapping
    @Operation(summary = "Actualiza perfil y preferencias")
    public ResponseEntity<Void> update(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        updateProfileService.update(
                userId(jwt),
                new UpdateProfileCommand(
                        request.displayName(),
                        request.locale(),
                        request.unitSystem(),
                        request.theme(),
                        request.defaultRestSeconds(),
                        request.timerSound(),
                        request.emailWeeklySummary()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/password")
    @Operation(summary = "Cambia la contraseña y revoca las demás sesiones")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @CookieValue(name = RefreshProperties.COOKIE_NAME, required = false) String refreshToken,
            @Valid @RequestBody ChangePasswordRequest request) {
        changePasswordService.change(userId(jwt), request.current(), request.next(), refreshToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @Operation(summary = "Exporta todos los datos del usuario (RGPD)")
    public ResponseEntity<java.util.Map<String, Object>> export(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ferro-export.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dataExportService.export(userId(jwt)));
    }

    @DeleteMapping
    @Operation(summary = "Elimina la cuenta y todos sus datos")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) DeleteAccountRequest request) {
        deleteAccountService.delete(userId(jwt), request == null ? null : request.confirmPassword());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
                .build();
    }

    private UUID userId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidCredentialsException();
        }
    }

    private ResponseCookie clearedRefreshCookie() {
        return ResponseCookie.from(RefreshProperties.COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshProperties.cookieSecure())
                .sameSite(refreshProperties.cookieSameSite())
                .path("/api/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
