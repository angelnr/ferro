package com.ferro.training.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ferro.platform.api.FerroMetrics;
import com.ferro.training.api.SessionCompleted;
import com.ferro.training.api.SessionStarted;
import com.ferro.training.api.SetLogged;

/** Consumidor de eventos de training para métricas (RF-085). */
@Component
public class TrainingMetricsListener {

    private final FerroMetrics metrics;

    public TrainingMetricsListener(FerroMetrics metrics) {
        this.metrics = metrics;
    }

    @EventListener
    void onSessionStarted(SessionStarted event) {
        metrics.sessionStarted();
    }

    @EventListener
    void onSetLogged(SetLogged event) {
        metrics.setLogged();
    }

    @EventListener
    void onSessionCompleted(SessionCompleted event) {
        metrics.sessionFinished();
        for (int index = 0; index < event.newPersonalRecords(); index++) {
            metrics.personalRecord();
        }
    }
}
