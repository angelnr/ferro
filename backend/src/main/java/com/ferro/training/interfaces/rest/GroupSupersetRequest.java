package com.ferro.training.interfaces.rest;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record GroupSupersetRequest(@NotEmpty @Size(max = 5) List<UUID> entryIds, Integer group) {}
