package com.ferro.training.interfaces.rest;

import jakarta.validation.constraints.Size;

public record UpdateSessionRequest(
        @Size(max = 80) String name, @Size(max = 500) String notes) {}
