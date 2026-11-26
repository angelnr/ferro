package com.ferro.identity.interfaces.rest;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ferro.identity.application.AuthResult;
import com.ferro.identity.application.oauth.OAuthAuthorizeService;
import com.ferro.identity.application.oauth.OAuthLoginService;
import com.ferro.identity.domain.OAuthProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/v1/auth/oauth")
@Tag(name = "Auth")
public class OAuthController {

    private final OAuthAuthorizeService authorizeService;
    private final OAuthLoginService loginService;
    private final RefreshCookieFactory cookieFactory;

    public OAuthController(
            OAuthAuthorizeService authorizeService,
            OAuthLoginService loginService,
            RefreshCookieFactory cookieFactory) {
        this.authorizeService = authorizeService;
        this.loginService = loginService;
        this.cookieFactory = cookieFactory;
    }

    @GetMapping("/providers")
    @Operation(summary = "Proveedores OAuth configurados")
    public Map<String, Boolean> providers() {
        return authorizeService.enabledProviders();
    }

    @GetMapping("/{provider}/authorize-url")
    @Operation(summary = "URL de autorización OAuth con PKCE y state firmado")
    public Map<String, String> authorizeUrl(@PathVariable String provider) {
        return Map.of("url", authorizeService.authorizeUrl(OAuthProvider.fromPath(provider)));
    }

    @PostMapping("/{provider}/callback")
    @Operation(summary = "Canjea el code OAuth y crea/vincula la cuenta")
    public ResponseEntity<AuthResponse> callback(
            @PathVariable String provider, @Valid @RequestBody OAuthCallbackRequest request) {
        AuthResult result = loginService.login(OAuthProvider.fromPath(provider), request.code(), request.state());
        return ResponseEntity.status(HttpStatus.OK)
                .header(
                        HttpHeaders.SET_COOKIE,
                        cookieFactory.create(result.refreshToken()).toString())
                .body(AuthResponse.from(result));
    }

    public record OAuthCallbackRequest(
            @NotBlank String code, @NotBlank String state) {}
}
