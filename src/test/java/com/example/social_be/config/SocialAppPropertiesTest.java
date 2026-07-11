package com.example.social_be.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class SocialAppPropertiesTest {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(TestConfig.class);

  private static final String[] VALID_PROPS = {
      "social-app.jwt.secret=access-secret",
      "social-app.jwt.refresh-secret=refresh-secret",
      "social-app.jwt.access-ttl=86400",
      "social-app.jwt.refresh-ttl=604800",
      "social-app.cloudinary.cloud-name=demo",
      "social-app.cloudinary.cloud-api-key=key",
      "social-app.cloudinary.cloud-secret-key=secret",
      "social-app.cors.allowed-origins=http://localhost:3000"
  };

  @Test
  void bindsSuccessfully_whenAllRequiredValuesPresent() {
    runner.withPropertyValues(VALID_PROPS).run(context -> {
      assertThat(context).hasNotFailed();
      SocialAppProperties props = context.getBean(SocialAppProperties.class);
      assertThat(props.getJwt().getSecret()).isEqualTo("access-secret");
      assertThat(props.getJwt().getAccessTtl()).isEqualTo(86400L);
      assertThat(props.getCors().getAllowedOrigins()).containsExactly("http://localhost:3000");
      // cookie.secure has a default and needs no explicit config
      assertThat(props.getCookie().isSecure()).isTrue();
    });
  }

  @Test
  void failsFast_whenJwtSecretMissing() {
    runner.withPropertyValues(
        "social-app.jwt.refresh-secret=refresh-secret",
        "social-app.jwt.access-ttl=86400",
        "social-app.jwt.refresh-ttl=604800",
        "social-app.cloudinary.cloud-name=demo",
        "social-app.cloudinary.cloud-api-key=key",
        "social-app.cloudinary.cloud-secret-key=secret",
        "social-app.cors.allowed-origins=http://localhost:3000")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void failsFast_whenCorsOriginsEmpty() {
    runner.withPropertyValues(
        "social-app.jwt.secret=access-secret",
        "social-app.jwt.refresh-secret=refresh-secret",
        "social-app.jwt.access-ttl=86400",
        "social-app.jwt.refresh-ttl=604800",
        "social-app.cloudinary.cloud-name=demo",
        "social-app.cloudinary.cloud-api-key=key",
        "social-app.cloudinary.cloud-secret-key=secret")
        .run(context -> assertThat(context).hasFailed());
  }

  @Configuration
  @EnableConfigurationProperties(SocialAppProperties.class)
  static class TestConfig {
  }
}
