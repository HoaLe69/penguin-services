package com.example.social_be.util;

import com.example.social_be.config.SocialAppProperties;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenUtil {
  @Autowired
  private SocialAppProperties properties;

  public String generateToken(String userName, String secretKey, long expireTime) {
    return Jwts.builder().setSubject(userName).setIssuedAt(new Date(System.currentTimeMillis()))
        .setExpiration(new Date(System.currentTimeMillis() + expireTime * 1000))
        .signWith(SignatureAlgorithm.HS512, secretKey)
        .compact();
  }

  public String getUserNameFromClamis(String token, String secretKey) {
    final Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    return claims.getSubject();
  }

  public String generateJwtAccessToken(String userName) {
    return this.generateToken(userName, properties.getJwt().getSecret(), properties.getJwt().getAccessTtl());
  }

  public String generateJwtRefreshToken(String userName) {
    return this.generateToken(userName, properties.getJwt().getRefreshSecret(), properties.getJwt().getRefreshTtl());
  }

  public boolean validateToken(String userName, String token, String secretKey) {
    String username = this.getUserNameFromClamis(token, secretKey);
    final Claims claims = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token).getBody();
    boolean isTokenExpried = claims.getExpiration().before(new Date());
    return (userName.equals(username) && !isTokenExpried);
  }

  public boolean validateJwtAccessToken(String token, String userName) {
    return this.validateToken(userName, token, properties.getJwt().getSecret());
  }

  public boolean validateJwtRefreshToken(String token, String userName) {
    return this.validateToken(userName, token, properties.getJwt().getRefreshSecret());
  }

  public String getUserNameFromAccessToken(String token) {
    return this.getUserNameFromClamis(token, properties.getJwt().getSecret());
  }

  public String getUserNameFromRefreshToken(String token) {
    return this.getUserNameFromClamis(token, properties.getJwt().getRefreshSecret());
  }

  public long getAccessTokenValiditySeconds() {
    return properties.getJwt().getAccessTtl();
  }

  public long getRefreshTokenValiditySeconds() {
    return properties.getJwt().getRefreshTtl();
  }
}
