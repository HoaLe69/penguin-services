package com.example.social_be.config;

import com.example.social_be.service.UserService;
import com.example.social_be.util.JwtTokenUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private JwtTokenUtil jwtTokenUtil;

  @Autowired
  private UserService userService;

  private String getCookie(String cookieName, Cookie[] cookies) {
    if (cookies == null) {
      return "";
    }
    for (Cookie cookie : cookies) {
      if (cookieName.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return "";
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!request.getServletPath().contains("/api/auth")) {
      String token = getCookie("token", request.getCookies());

      if (StringUtils.hasText(token)) {
        try {
          String userName = jwtTokenUtil.getUserNameFromAccessToken(token);
          UserDetails userDetails = userService.loadUserByUsername(userName);

          if (userDetails != null
              && jwtTokenUtil.validateJwtAccessToken(token, userDetails.getUsername())) {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, new ArrayList<>());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
          }
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException ex) {
          // Expired/malformed/unsigned token or unknown user: leave the request
          // unauthenticated so Spring Security's entry point returns 401 for
          // protected paths, instead of a 500 from an unhandled filter exception.
          log.debug("Rejecting invalid access token: {}", ex.getMessage());
          SecurityContextHolder.clearContext();
        }
      }
    }
    filterChain.doFilter(request, response);
  }
}
