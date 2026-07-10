package com.example.social_be.exception;

/**
 * Thrown when the authenticated caller is not allowed to act on a resource
 * (e.g. editing another user's post). Maps to HTTP 403.
 */
public class ForbiddenException extends RuntimeException {
  public ForbiddenException(String message) {
    super(message);
  }
}
