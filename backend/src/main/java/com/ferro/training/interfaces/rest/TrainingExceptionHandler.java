package com.ferro.training.interfaces.rest;

import java.net.URI;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ferro.training.domain.InvalidSessionStateException;
import com.ferro.training.domain.InvalidTrainingException;
import com.ferro.training.domain.SessionAlreadyActiveException;
import com.ferro.training.domain.SessionNotFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrainingExceptionHandler {

    @ExceptionHandler(SessionAlreadyActiveException.class)
    ResponseEntity<ProblemDetail> alreadyActive(SessionAlreadyActiveException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Sesión ya activa");
        problem.setType(URI.create("https://ferro.dev/problems/session-already-active"));
        problem.setProperty("code", "SESSION_ALREADY_ACTIVE");
        if (exception.activeSessionId() != null) {
            problem.setProperty("activeSessionId", exception.activeSessionId().toString());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(SessionNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(SessionNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("No encontrado");
        problem.setType(URI.create("https://ferro.dev/problems/not-found"));
        problem.setProperty("code", "NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(InvalidSessionStateException.class)
    ResponseEntity<ProblemDetail> invalidState(InvalidSessionStateException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Sesión no activa");
        problem.setType(URI.create("https://ferro.dev/problems/invalid-session-state"));
        problem.setProperty("code", "INVALID_SESSION_STATE");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(InvalidTrainingException.class)
    ResponseEntity<ProblemDetail> invalid(InvalidTrainingException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problem.setTitle("Datos de entrenamiento no válidos");
        problem.setType(URI.create("https://ferro.dev/problems/validation"));
        problem.setProperty("code", "VALIDATION_FAILED");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
