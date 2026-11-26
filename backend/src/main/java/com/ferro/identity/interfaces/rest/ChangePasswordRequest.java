package com.ferro.identity.interfaces.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String current,
        @NotBlank @Size(min = 10, max = 128) String next) {}
