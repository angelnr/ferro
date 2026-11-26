package com.ferro.identity.application;

public record LoginCommand(String email, String rawPassword) {}
