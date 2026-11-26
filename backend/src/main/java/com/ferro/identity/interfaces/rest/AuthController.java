package com.ferro.identity.interfaces.rest;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ferro.identity.application.AuthResult;
import com.ferro.identity.application.GetCurrentUserService;
import com.ferro.identity.application.LoginCommand;
import com.ferro.identity.application.LoginService;
import com.ferro.identity.application.LogoutService;
import com.ferro.identity.application.RefreshProperties;
import com.ferro.identity.application.RefreshService;
import com.ferro.identity.application.RegisterCommand;
import com.ferro.identity.application.RegisterUserService;
import com.ferro.identity.domain.InvalidCredentialsException;
import com.ferro.identity.domain.InvalidOriginException;
import com.ferro.platform.api.RateLimiter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final RegisterUserService registerUserService;
    private final LoginService loginService;
    private final RefreshService refreshService;
    private final LogoutService logoutService;
    private final GetCurrentUserService getCurrentUserService;
    private final RefreshProperties refreshProperties;
    private final RateLimiter rateLimiter;
    private final Set<String> allowedOrigins;

    public AuthController(
            RegisterUserService registerUserService,
            LoginService loginService,
            RefreshService refreshService,
            LogoutService logoutService,
            GetCurrentUserService getCurrentUserService,
            RefreshProperties refreshProperties,
            RateLimiter rateLimiter,
            @Value("${ferro.security.cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        this.registerUserService = registerUserService;
        this.loginService = loginService;
        this.refreshService = refreshService;
        this.logoutService = logoutService;
        this.getCurrentUserService = getCurrentUserService;
        this.refreshProperties = refreshProperties;
        this.rateLimiter = rateLimiter;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @PostMapping("/register")
    @Operation(summary = "Registra una cuenta email+contraseña")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            HttpServletRequest httpRequest) {
        rateLimiter.checkRegister(httpRequest.getRemoteAddr());
        validateOrigin(origin);
        AuthResult result = registerUserService.register(new RegisterCommand(
                request.displayName(),
                request.email(),
                request.password(),
                Boolean.TRUE.equals(request.healthConsent()),
                request.locale(),
                request.unitSystem()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken()).toString())
                .body(AuthResponse.from(result));
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión con email+contraseña")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "Origin", required = false) String origin,
            HttpServletRequest httpRequest) {
        rateLimiter.checkLogin(httpRequest.getRemoteAddr(), request.email());
        validateOrigin(origin);
        AuthResult result = loginService.login(new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken()).toString())
                .body(AuthResponse.from(result));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rota el refresh token de la cookie")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshProperties.COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(value = "Origin", required = false) String origin,
            HttpServletRequest httpRequest) {
        rateLimiter.checkRefresh(httpRequest.getRemoteAddr());
        validateOrigin(origin);
        AuthResult result = refreshService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken()).toString())
                .body(AuthResponse.from(result));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoca el refresh token actual")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshProperties.COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(value = "Origin", required = false) String origin) {
        validateOrigin(origin);
        logoutService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
                .build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Revoca todos los refresh tokens del usuario")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidCredentialsException();
        }
        logoutService.logoutAll(userId);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
                .build();
    }

    @GetMapping("/me")
    @Operation(summary = "Devuelve el usuario y sus preferencias")
    @SecurityRequirement(name = "bearerAuth")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new InvalidCredentialsException();
        }
        return MeResponse.from(getCurrentUserService.byId(userId));
    }

    private void validateOrigin(String origin) {
        if (origin != null && !origin.isBlank() && !allowedOrigins.contains(origin)) {
            throw new InvalidOriginException();
        }
    }

    private ResponseCookie refreshCookie(String token) {
        return ResponseCookie.from(RefreshProperties.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(refreshProperties.cookieSecure())
                .sameSite(refreshProperties.cookieSameSite())
                .path("/api/v1/auth")
                .maxAge(refreshProperties.ttl())
                .build();
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
