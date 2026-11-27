package com.ferro.identity.interfaces.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 80) String displayName,

        @Pattern(regexp = "es|en", message = "locale debe ser es o en")
        String locale,

        @Pattern(regexp = "KG|LB", message = "unitSystem debe ser KG o LB")
        String unitSystem,

        @Pattern(regexp = "DARK|LIGHT", message = "theme debe ser DARK o LIGHT")
        String theme,

        @Min(15) @Max(600) Integer defaultRestSeconds,
        Boolean timerSound,
        Boolean emailWeeklySummary) {}
