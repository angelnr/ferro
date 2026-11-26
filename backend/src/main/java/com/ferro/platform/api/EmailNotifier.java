package com.ferro.platform.api;

/** Notificaciones de plataforma que otros módulos pueden solicitar sin conocer identidad. */
public interface EmailNotifier {

    void sendSessionAutoClosed(String recipientEmail, String locale);
}
