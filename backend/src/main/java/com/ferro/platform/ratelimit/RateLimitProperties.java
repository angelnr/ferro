package com.ferro.platform.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ferro.rate-limit")
public record RateLimitProperties(@DefaultValue("true") boolean enabled) {}
