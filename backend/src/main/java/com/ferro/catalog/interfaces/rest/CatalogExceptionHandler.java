package com.ferro.catalog.interfaces.rest;

import java.net.URI;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ferro.catalog.domain.ExerciseInUseException;
import com.ferro.catalog.domain.ExerciseLimitReachedException;
import com.ferro.catalog.domain.ExerciseNotFoundException;
import com.ferro.catalog.domain.InvalidExerciseException;
import com.ferro.catalog.domain.InvalidFilterException;
import com.ferro.shared.domain.InvalidPrincipalException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CatalogExceptionHandler {

    @ExceptionHandler(InvalidPrincipalException.class)
    ResponseEntity<ProblemDetail> invalidPrincipal(InvalidPrincipalException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
        problem.setTitle("No autorizado");
        problem.setType(URI.create("https://ferro.dev/problems/unauthorized"));
        problem.setProperty("code", "UNAUTHORIZED");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ExerciseNotFoundException.class)
    ResponseEntity<ProblemDetail> notFound(ExerciseNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("No encontrado");
        problem.setType(URI.create("https://ferro.dev/problems/not-found"));
        problem.setProperty("code", "NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ExerciseInUseException.class)
    ResponseEntity<ProblemDetail> inUse(ExerciseInUseException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Ejercicio en uso");
        problem.setType(URI.create("https://ferro.dev/problems/exercise-in-use"));
        problem.setProperty("code", "EXERCISE_IN_USE");
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ExerciseLimitReachedException.class)
    ResponseEntity<ProblemDetail> limit(ExerciseLimitReachedException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problem.setTitle("Límite alcanzado");
        problem.setType(URI.create("https://ferro.dev/problems/limit-reached"));
        problem.setProperty("code", "LIMIT_REACHED");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(InvalidExerciseException.class)
    ResponseEntity<ProblemDetail> invalidExercise(InvalidExerciseException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problem.setTitle("Ejercicio no válido");
        problem.setType(URI.create("https://ferro.dev/problems/validation"));
        problem.setProperty("code", "VALIDATION_FAILED");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(InvalidFilterException.class)
    ResponseEntity<ProblemDetail> invalidFilter(InvalidFilterException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Filtro no válido");
        problem.setType(URI.create("https://ferro.dev/problems/invalid-filter"));
        problem.setProperty("code", "INVALID_FILTER");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
