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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserService registerUserService;
    private final LoginService loginService;
    private final RefreshService refreshService;
    private final LogoutService logoutService;
    private final GetCurrentUserService getCurrentUserService;
    private final RefreshProperties refreshProperties;
    private final Set<String> allowedOrigins;

    public AuthController(
            RegisterUserService registerUserService,
            LoginService loginService,
            RefreshService refreshService,
            LogoutService logoutService,
            GetCurrentUserService getCurrentUserService,
            RefreshProperties refreshProperties,
            @Value("${ferro.security.cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        this.registerUserService = registerUserService;
        this.loginService = loginService;
        this.refreshService = refreshService;
        this.logoutService = logoutService;
        this.getCurrentUserService = getCurrentUserService;
        this.refreshProperties = refreshProperties;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "Origin", required = false) String origin) {
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
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "Origin", required = false) String origin) {
        validateOrigin(origin);
        AuthResult result = loginService.login(new LoginCommand(request.email(), request.password()));
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken()).toString())
                .body(AuthResponse.from(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshProperties.COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(value = "Origin", required = false) String origin) {
        validateOrigin(origin);
        AuthResult result = refreshService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie(result.refreshToken()).toString())
                .body(AuthResponse.from(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshProperties.COOKIE_NAME, required = false) String refreshToken,
            @RequestHeader(value = "Origin", required = false) String origin) {
        validateOrigin(origin);
        logoutService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie().toString())
                .build();
    }

    @GetMapping("/me")
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
