package com.example.social_be.util;

import jakarta.servlet.http.Cookie;

import jakarta.servlet.http.HttpServletResponse;

public class AuthUtil {
  public void attachTokenInCookieResponse(HttpServletResponse response, String accessToken, String refreshToken) {

    Cookie accessTokenCookie = new Cookie("token", accessToken);
    accessTokenCookie.setHttpOnly(true);  // Prevent JavaScript access to the cookie
    accessTokenCookie.setSecure(true);  // Only send the cookie over HTTPS
    accessTokenCookie.setMaxAge(60 * 60 * 24 * 7);  // 7 days
    accessTokenCookie.setPath("/");  // Cookie is available for all paths

    Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
    refreshTokenCookie.setHttpOnly(true);  // Prevent JavaScript access to the cookie
    refreshTokenCookie.setSecure(true);  // Only send the cookie over HTTPS
    refreshTokenCookie.setMaxAge(60 * 60 * 24 * 7);  // 7 days
    refreshTokenCookie.setPath("/");  // Cookie is available for all paths

// Add cookies to the response
    response.addCookie(accessTokenCookie);
    response.addCookie(refreshTokenCookie);
  }
}
