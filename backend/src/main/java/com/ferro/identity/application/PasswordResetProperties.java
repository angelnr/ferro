package com.ferro.identity.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ferro.password-reset")
public record PasswordResetProperties(@DefaultValue("30m") Duration ttl) {}
