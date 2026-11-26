package com.ferro.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("ferro.app")
public record AppProperties(
        @DefaultValue("http://localhost:4200") String frontendUrl) {}
