package com.ferro.platform.api;

/** Notificaciones de plataforma que otros módulos pueden solicitar sin conocer identidad. */
public interface EmailNotifier {

    void sendSessionAutoClosed(String recipientEmail, String locale);

    void sendWeeklySummary(
            String recipientEmail,
            String locale,
            int sessions,
            java.math.BigDecimal volume,
            int personalRecords,
            int streak);
}
