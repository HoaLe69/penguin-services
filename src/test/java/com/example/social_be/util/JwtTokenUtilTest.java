package com.example.social_be.util;

import com.example.social_be.config.SocialAppProperties;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenUtilTest {

  // >= 64 chars, required for HS512.
  private static final String ACCESS_SECRET = "access-secret-0123456789abcdef0123456789abcdef0123456789abcdef01";
  private static final String REFRESH_SECRET = "refresh-secret-0123456789abcdef0123456789abcdef0123456789abcdef0";

  private JwtTokenUtil jwtTokenUtil;

  @BeforeEach
  void setup() {
    SocialAppProperties properties = new SocialAppProperties();
    properties.getJwt().setSecret(ACCESS_SECRET);
    properties.getJwt().setRefreshSecret(REFRESH_SECRET);
    properties.getJwt().setAccessTtl(86400);
    properties.getJwt().setRefreshTtl(604800);

    jwtTokenUtil = new JwtTokenUtil();
    ReflectionTestUtils.setField(jwtTokenUtil, "properties", properties);
  }

  @Test
  void accessToken_roundTrips_usernameAndValidation() {
    String token = jwtTokenUtil.generateJwtAccessToken("alice");

    assertThat(jwtTokenUtil.getUserNameFromAccessToken(token)).isEqualTo("alice");
    assertThat(jwtTokenUtil.validateJwtAccessToken(token, "alice")).isTrue();
    assertThat(jwtTokenUtil.validateJwtAccessToken(token, "someone-else")).isFalse();
  }

  @Test
  void refreshToken_roundTrips_usernameAndValidation() {
    String token = jwtTokenUtil.generateJwtRefreshToken("bob");

    assertThat(jwtTokenUtil.getUserNameFromRefreshToken(token)).isEqualTo("bob");
    assertThat(jwtTokenUtil.validateJwtRefreshToken(token, "bob")).isTrue();
  }

  @Test
  void accessToken_cannotBeValidatedAsRefreshToken_differentSecrets() {
    String accessToken = jwtTokenUtil.generateJwtAccessToken("alice");

    assertThatThrownBy(() -> jwtTokenUtil.getUserNameFromRefreshToken(accessToken))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void tamperedToken_failsVerification() {
    String token = jwtTokenUtil.generateJwtAccessToken("alice");
    // Flip a character in the payload segment (not the very edge of the
    // token, where a base64 boundary could occasionally leave the decoded
    // bytes unchanged) - any payload change must invalidate the signature.
    int mid = token.length() / 2;
    char flipped = token.charAt(mid) == 'a' ? 'b' : 'a';
    String tampered = token.substring(0, mid) + flipped + token.substring(mid + 1);

    assertThatThrownBy(() -> jwtTokenUtil.getUserNameFromAccessToken(tampered))
        .isInstanceOf(JwtException.class);
  }
}
