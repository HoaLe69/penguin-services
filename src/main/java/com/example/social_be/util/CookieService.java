package com.example.social_be.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for setting/clearing the "token" and "refreshToken"
 * auth cookies. SameSite=None requires Secure, so when cookieSecure is
 * relaxed off (local http dev) SameSite falls back to Lax instead of an
 * invalid None-without-Secure combination.
 */
@Component
public class CookieService {

  public static final String ACCESS_TOKEN_COOKIE = "token";
  public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

  @Value("${social_app.cookieSecure:true}")
  private boolean secure;

  public void attachAccessTokenCookie(HttpServletResponse response, String accessToken, long maxAgeSeconds) {
    addCookie(response, ACCESS_TOKEN_COOKIE, accessToken, maxAgeSeconds);
  }

  public void attachRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
    addCookie(response, REFRESH_TOKEN_COOKIE, refreshToken, maxAgeSeconds);
  }

  public void attachAuthCookies(HttpServletResponse response, String accessToken, long accessMaxAgeSeconds,
      String refreshToken, long refreshMaxAgeSeconds) {
    attachAccessTokenCookie(response, accessToken, accessMaxAgeSeconds);
    attachRefreshTokenCookie(response, refreshToken, refreshMaxAgeSeconds);
  }

  public void clearAuthCookies(HttpServletResponse response) {
    addCookie(response, ACCESS_TOKEN_COOKIE, "", 0);
    addCookie(response, REFRESH_TOKEN_COOKIE, "", 0);
  }

  private void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
    ResponseCookie cookie = ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(secure)
        .sameSite(secure ? "None" : "Lax")
        .path("/")
        .maxAge(maxAgeSeconds)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }
}
