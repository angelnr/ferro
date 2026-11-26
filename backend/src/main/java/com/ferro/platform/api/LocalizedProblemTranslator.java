package com.ferro.platform.api;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Traduce ProblemDetail al Accept-Language de la petición actual. */
@Component
public class LocalizedProblemTranslator {

    private final MessageSource messages;

    public LocalizedProblemTranslator(MessageSource messages) {
        this.messages = messages;
    }

    public ProblemDetail translate(ProblemDetail problem) {
        Object codeValue =
                problem.getProperties() == null ? null : problem.getProperties().get("code");
        if (codeValue == null) {
            return problem;
        }
        String code = String.valueOf(codeValue);
        Locale locale = locale();
        problem.setDetail(messages.getMessage("error." + code, null, problem.getDetail(), locale));
        String title = messages.getMessage("error." + code + ".title", null, problem.getTitle(), locale);
        if (title != null) {
            problem.setTitle(title);
        }
        return problem;
    }

    private Locale locale() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String header = attributes.getRequest().getHeader("Accept-Language");
            if (header != null && header.toLowerCase(Locale.ROOT).startsWith("en")) {
                return Locale.ENGLISH;
            }
        }
        return Locale.forLanguageTag("es");
    }
}
