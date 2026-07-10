package com.example.social_be.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error payload returned for every handled exception.
 * Shape is stable so the frontend can rely on it.
 */
public record ErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    Map<String, String> fieldErrors) {

  public static ErrorResponse of(int status, String code, String message, String path) {
    return new ErrorResponse(Instant.now(), status, code, message, path, null);
  }

  public static ErrorResponse of(int status, String code, String message, String path,
      Map<String, String> fieldErrors) {
    return new ErrorResponse(Instant.now(), status, code, message, path, fieldErrors);
  }
}
