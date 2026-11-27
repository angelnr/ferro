package com.ferro.platform.api;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/** Métricas de negocio de RF-085. */
@Component
public class FerroMetrics {

    private final Counter sessionsStarted;
    private final Counter sessionsFinished;
    private final Counter setsLogged;
    private final Counter personalRecords;
    private final MeterRegistry registry;

    public FerroMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.sessionsStarted = Counter.builder("ferro.sessions.started").register(registry);
        this.sessionsFinished = Counter.builder("ferro.sessions.finished").register(registry);
        this.setsLogged = Counter.builder("ferro.sets.logged").register(registry);
        this.personalRecords = Counter.builder("ferro.prs").register(registry);
    }

    public void sessionStarted() {
        sessionsStarted.increment();
    }

    public void sessionFinished() {
        sessionsFinished.increment();
    }

    public void setLogged() {
        setsLogged.increment();
    }

    public void personalRecord() {
        personalRecords.increment();
    }

    public void emailSent(String template) {
        Counter.builder("ferro.emails.sent")
                .tag("template", template)
                .register(registry)
                .increment();
    }
}
