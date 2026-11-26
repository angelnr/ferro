package com.ferro.identity.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ferro.email-verification")
public record EmailVerificationProperties(
        @DefaultValue("24h") Duration ttl) {}
