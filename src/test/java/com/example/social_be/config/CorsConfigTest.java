package com.example.social_be.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

  @Test
  void corsConfigurationSource_usesConfiguredOrigins_andAllowsCredentials() {
    CorsConfig corsConfig = new CorsConfig();
    SocialAppProperties properties = new SocialAppProperties();
    properties.getCors().setAllowedOrigins(List.of("https://penguin-brown-eight.vercel.app", "http://localhost:3000"));
    ReflectionTestUtils.setField(corsConfig, "properties", properties);

    CorsConfigurationSource source = corsConfig.corsConfigurationSource();
    HttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/verify");
    CorsConfiguration configuration = source.getCorsConfiguration(request);

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOrigins())
        .containsExactly("https://penguin-brown-eight.vercel.app", "http://localhost:3000");
    assertThat(configuration.getAllowCredentials()).isTrue();
    assertThat(configuration.getExposedHeaders()).contains("Set-Cookie");
  }
}
