package com.ferro.progress.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ferro.training.api.SessionCompleted;
import com.ferro.training.api.SessionDiscarded;
import com.ferro.training.api.SessionEdited;
import com.ferro.training.api.SetLogged;

/** Invalida el read model de progreso cuando cambia una sesión. */
@Component
public class ProgressEventListener {

    private final ProgressCache cache;

    public ProgressEventListener(ProgressCache cache) {
        this.cache = cache;
    }

    @EventListener
    void onCompleted(SessionCompleted event) {
        cache.invalidate(event.userId());
    }

    @EventListener
    void onDiscarded(SessionDiscarded event) {
        cache.invalidate(event.userId());
    }

    @EventListener
    void onEdited(SessionEdited event) {
        cache.invalidate(event.userId());
    }

    @EventListener
    void onSetLogged(SetLogged event) {
        cache.invalidate(event.userId());
    }
}
