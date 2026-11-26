package com.ferro.identity.interfaces.rest;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ferro.identity.domain.AccountLockedException;
import com.ferro.identity.domain.EmailTakenException;
import com.ferro.identity.domain.EmailUnverifiedException;
import com.ferro.identity.domain.HealthConsentRequiredException;
import com.ferro.identity.domain.InvalidCredentialsException;
import com.ferro.identity.domain.InvalidDisplayNameException;
import com.ferro.identity.domain.InvalidEmailException;
import com.ferro.identity.domain.InvalidLocaleException;
import com.ferro.identity.domain.InvalidOriginException;
import com.ferro.identity.domain.InvalidPasswordException;
import com.ferro.identity.domain.InvalidProfileException;
import com.ferro.identity.domain.InvalidUnitSystemException;
import com.ferro.identity.domain.PasswordResetInvalidException;
import com.ferro.identity.domain.RefreshInvalidException;
import com.ferro.identity.domain.VerificationInvalidException;
import com.ferro.shared.domain.DomainException;
import com.ferro.shared.domain.RateLimitExceededException;

/** Traduce excepciones a RFC 7807 con un `code` estable para la UI y los tests. */
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException exception) {
        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(error.getField(), key -> new java.util.ArrayList<>())
                    .add(error.getDefaultMessage());
        }
        ProblemDetail problem =
                problem(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", "La petición no es válida");
        problem.setProperty("errors", errors);
        return response(problem);
    }

    @ExceptionHandler({
        InvalidEmailException.class,
        InvalidPasswordException.class,
        InvalidDisplayNameException.class,
        InvalidLocaleException.class,
        InvalidUnitSystemException.class,
        InvalidProfileException.class,
        HealthConsentRequiredException.class
    })
    ResponseEntity<ProblemDetail> invalidInput(DomainException exception) {
        return response(problem(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDATION_FAILED",
                "La petición no es válida",
                exception.getMessage()));
    }

    @ExceptionHandler(EmailTakenException.class)
    ResponseEntity<ProblemDetail> emailTaken(EmailTakenException exception) {
        return response(problem(HttpStatus.CONFLICT, "EMAIL_TAKEN", exception.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> invalidCredentials(InvalidCredentialsException exception) {
        return response(problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage()));
    }

    @ExceptionHandler(RefreshInvalidException.class)
    ResponseEntity<ProblemDetail> refreshInvalid(RefreshInvalidException exception) {
        return response(problem(HttpStatus.UNAUTHORIZED, "REFRESH_INVALID", exception.getMessage()));
    }

    @ExceptionHandler(VerificationInvalidException.class)
    ResponseEntity<ProblemDetail> verificationInvalid(VerificationInvalidException exception) {
        return response(problem(HttpStatus.BAD_REQUEST, "VERIFICATION_INVALID", exception.getMessage()));
    }

    @ExceptionHandler(PasswordResetInvalidException.class)
    ResponseEntity<ProblemDetail> passwordResetInvalid(PasswordResetInvalidException exception) {
        return response(problem(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_INVALID", exception.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ProblemDetail> rateLimited(RateLimitExceededException exception) {
        ProblemDetail problem = problem(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", exception.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", Long.toString(exception.retryAfterSeconds()))
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(AccountLockedException.class)
    ResponseEntity<ProblemDetail> accountLocked(AccountLockedException exception) {
        ProblemDetail problem = problem(
                HttpStatus.FORBIDDEN,
                "ACCOUNT_LOCKED",
                exception.getMessage(),
                "La cuenta está bloqueada hasta " + exception.lockedUntil());
        problem.setProperty("lockedUntil", exception.lockedUntil().toString());
        return response(problem);
    }

    @ExceptionHandler(EmailUnverifiedException.class)
    ResponseEntity<ProblemDetail> emailUnverified(EmailUnverifiedException exception) {
        return response(problem(HttpStatus.FORBIDDEN, "EMAIL_UNVERIFIED", exception.getMessage()));
    }

    @ExceptionHandler(InvalidOriginException.class)
    ResponseEntity<ProblemDetail> invalidOrigin(InvalidOriginException exception) {
        return response(problem(HttpStatus.FORBIDDEN, "ORIGIN_NOT_ALLOWED", exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> malformedJson(HttpMessageNotReadableException exception) {
        return response(problem(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "El cuerpo JSON no es válido"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> dataIntegrity(DataIntegrityViolationException exception) {
        String message = String.valueOf(exception.getMostSpecificCause().getMessage());
        if (message.contains("users_email_key") || message.contains("duplicate key")) {
            return response(problem(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Ya existe una cuenta con ese email"));
        }
        return response(problem(HttpStatus.CONFLICT, "CONFLICT", "Conflicto al guardar los datos"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(Exception exception) {
        return response(
                problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Error interno, inténtalo más tarde"));
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail) {
        return problem(status, code, status.getReasonPhrase(), detail);
    }

    private ProblemDetail problem(HttpStatus status, String code, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(
                URI.create("https://ferro.dev/problems/" + code.toLowerCase().replace('_', '-')));
        problem.setProperty("code", code);
        return problem;
    }

    private ResponseEntity<ProblemDetail> response(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
