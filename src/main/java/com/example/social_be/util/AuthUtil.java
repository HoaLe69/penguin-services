package com.example.social_be.util;

import jakarta.servlet.http.Cookie;

import jakarta.servlet.http.HttpServletResponse;

public class AuthUtil {
  public void attachTokenInCookieResponse(HttpServletResponse response, String accessToken, String refreshToken) {
    Cookie accessTokenCookie = new Cookie("token", accessToken);
    accessTokenCookie.setHttpOnly(true);
    accessTokenCookie.setSecure(true);
    accessTokenCookie.setDomain(".vercel.app");
    accessTokenCookie.setMaxAge(60 * 60 * 24 * 7);
    accessTokenCookie.setPath("/");

    Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
    refreshTokenCookie.setHttpOnly(true);
    refreshTokenCookie.setSecure(true);
    refreshTokenCookie.setDomain(".vercel.app");
    refreshTokenCookie.setMaxAge(60 * 60 * 24 * 7);
    refreshTokenCookie.setPath("/");
    //
    // add cookies to the response
    response.addCookie(accessTokenCookie);
    response.addCookie(refreshTokenCookie);
  }
}
