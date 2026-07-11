package com.example.social_be.exception;

import com.example.social_be.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Translates exceptions into the standard {@link ErrorResponse} shape with the
 * correct HTTP status. This is the single place errors are turned into responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
    return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req);
  }

  @ExceptionHandler({ ForbiddenException.class, AccessDeniedException.class })
  public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException ex, HttpServletRequest req) {
    return build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), req);
  }

  @ExceptionHandler({ BadCredentialsException.class, AuthenticationException.class })
  public ResponseEntity<ErrorResponse> handleUnauthorized(AuthenticationException ex, HttpServletRequest req) {
    return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid credentials", req);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    Map<String, String> fieldErrors = new HashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
    }
    ErrorResponse body = ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
        "Request validation failed", req.getRequestURI(), fieldErrors);
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
    return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req);
  }

  // Backstop for the unique indexes on UserCollection.userName/socialId: the
  // app-level check-then-insert in AuthController isn't atomic, so a
  // concurrent registration can still race past it and hit the DB
  // constraint. Surface it as a normal conflict, not a generic 500.
  @ExceptionHandler(DuplicateKeyException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateKey(DuplicateKeyException ex, HttpServletRequest req) {
    return build(HttpStatus.CONFLICT, "CONFLICT", "That value is already taken", req);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
    // Log the full detail here (once), but never leak internals to the client.
    log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred", req);
  }

  private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, HttpServletRequest req) {
    ErrorResponse body = ErrorResponse.of(status.value(), code, message, req.getRequestURI());
    return ResponseEntity.status(status).body(body);
  }
}
