package com.example.social_be.security;

import com.example.social_be.exception.ForbiddenException;
import com.example.social_be.model.custom.CustomUserDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helpers for reading the authenticated caller from the security context.
 * Identity must always come from the JWT principal here — never from a request
 * body or path variable — to prevent IDOR / spoofing.
 */
public final class SecurityUtils {

  private SecurityUtils() {
  }

  /** The authenticated principal, or a 403 if somehow absent. */
  public static CustomUserDetail currentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetail principal)) {
      throw new ForbiddenException("No authenticated user");
    }
    return principal;
  }

  /** The authenticated caller's user id (Mongo _id). */
  public static String currentUserId() {
    return currentUser().get_id();
  }

  /** Enforce that the caller is acting on their own account; 403 otherwise. */
  public static void requireSelf(String targetUserId) {
    if (!currentUserId().equals(targetUserId)) {
      throw new ForbiddenException("You are not allowed to act on behalf of another user");
    }
  }
}
