package com.ferro.catalog.interfaces.rest;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ferro.catalog.domain.InvalidFilterException;
import com.ferro.shared.domain.InvalidPrincipalException;

@RestControllerAdvice
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
