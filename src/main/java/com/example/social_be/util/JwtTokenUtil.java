package com.example.social_be.util;

import com.example.social_be.config.SocialAppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenUtil {
  @Autowired
  private SocialAppProperties properties;

  private SecretKey key(String secret) {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(String userName, String secret, long expireTime) {
    Date now = new Date();
    return Jwts.builder()
        .subject(userName)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + expireTime * 1000))
        .signWith(key(secret), Jwts.SIG.HS512)
        .compact();
  }

  public Claims getClaims(String token, String secret) {
    return Jwts.parser().verifyWith(key(secret)).build().parseSignedClaims(token).getPayload();
  }

  public String getUserNameFromClaims(String token, String secret) {
    return getClaims(token, secret).getSubject();
  }

  public String generateJwtAccessToken(String userName) {
    return this.generateToken(userName, properties.getJwt().getSecret(), properties.getJwt().getAccessTtl());
  }

  public String generateJwtRefreshToken(String userName) {
    return this.generateToken(userName, properties.getJwt().getRefreshSecret(), properties.getJwt().getRefreshTtl());
  }

  public boolean validateToken(String userName, String token, String secret) {
    // parseSignedClaims already throws ExpiredJwtException for an expired
    // token, but the explicit check is kept as a defensive backstop.
    Claims claims = getClaims(token, secret);
    boolean isTokenExpired = claims.getExpiration().before(new Date());
    return userName.equals(claims.getSubject()) && !isTokenExpired;
  }

  public boolean validateJwtAccessToken(String token, String userName) {
    return this.validateToken(userName, token, properties.getJwt().getSecret());
  }

  public boolean validateJwtRefreshToken(String token, String userName) {
    return this.validateToken(userName, token, properties.getJwt().getRefreshSecret());
  }

  public String getUserNameFromAccessToken(String token) {
    return this.getUserNameFromClaims(token, properties.getJwt().getSecret());
  }

  public String getUserNameFromRefreshToken(String token) {
    return this.getUserNameFromClaims(token, properties.getJwt().getRefreshSecret());
  }

  public long getAccessTokenValiditySeconds() {
    return properties.getJwt().getAccessTtl();
  }

  public long getRefreshTokenValiditySeconds() {
    return properties.getJwt().getRefreshTtl();
  }
}
