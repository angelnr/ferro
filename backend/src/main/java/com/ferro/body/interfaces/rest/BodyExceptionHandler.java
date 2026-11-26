package com.ferro.body.interfaces.rest;

import java.net.URI;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ferro.body.domain.BodyNotFoundException;
import com.ferro.body.domain.InvalidBodyException;
import com.ferro.body.domain.MeasurementExistsException;
import com.ferro.platform.api.LocalizedProblemTranslator;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BodyExceptionHandler {
    private final LocalizedProblemTranslator messages;

    public BodyExceptionHandler(LocalizedProblemTranslator messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BodyNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(BodyNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "No encontrada", exception.getMessage(), "not-found");
    }

    @ExceptionHandler(MeasurementExistsException.class)
    ResponseEntity<ProblemDetail> exists(MeasurementExistsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Medición duplicada");
        problem.setType(URI.create("https://ferro.dev/problems/measurement-exists"));
        problem.setProperty("code", "MEASUREMENT_EXISTS");
        problem.setProperty("measurementId", exception.measurementId().toString());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(messages.translate(problem));
    }

    @ExceptionHandler(InvalidBodyException.class)
    ResponseEntity<ProblemDetail> invalid(InvalidBodyException exception) {
        return problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDATION_FAILED",
                "Medición no válida",
                exception.getMessage(),
                "validation");
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String code, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://ferro.dev/problems/" + type));
        problem.setProperty("code", code);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(messages.translate(problem));
    }
}
