package com.pawmotionai.configuration;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pawmotionai.activity.domain.exception.ActivityNotFoundException;
import com.pawmotionai.activity.domain.exception.InvalidActivityException;
import com.pawmotionai.identity.domain.exception.InvalidCredentialsException;
import com.pawmotionai.identity.domain.exception.OwnerAlreadyExistsException;
import com.pawmotionai.identity.domain.exception.OwnerNotFoundException;
import com.pawmotionai.pet.domain.exception.InvalidPetException;
import com.pawmotionai.pet.domain.exception.PetNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Validation failed", detail);
    }

    @ExceptionHandler(OwnerAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleOwnerAlreadyExists(OwnerAlreadyExistsException exception) {
        return problem(HttpStatus.CONFLICT, "Owner already exists", exception.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsException.class, AuthenticationException.class})
    public ResponseEntity<ProblemDetail> handleAuthentication(RuntimeException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "Authentication failed", "Invalid credentials");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception) {
        return problem(HttpStatus.FORBIDDEN, "Access denied", exception.getMessage());
    }

    @ExceptionHandler({OwnerNotFoundException.class, PetNotFoundException.class, ActivityNotFoundException.class})
    public ResponseEntity<ProblemDetail> handleNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", exception.getMessage());
    }

    @ExceptionHandler({InvalidPetException.class, InvalidActivityException.class})
    public ResponseEntity<ProblemDetail> handleInvalidDomain(RuntimeException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid request", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(ProblemDetail.forStatus(status).getType());
        return ResponseEntity.status(status).body(problem);
    }
}
