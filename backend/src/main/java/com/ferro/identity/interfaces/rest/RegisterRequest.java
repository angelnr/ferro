package com.ferro.identity.interfaces.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 80) String displayName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 10, max = 128) String password,
        @NotNull Boolean healthConsent,

        @Pattern(regexp = "es|en", message = "locale debe ser es o en")
        String locale,

        @Pattern(regexp = "KG|LB", message = "unitSystem debe ser KG o LB")
        String unitSystem) {}
