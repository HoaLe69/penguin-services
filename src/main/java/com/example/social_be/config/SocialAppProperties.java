package com.example.social_be.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Typed, validated application config. Replaces scattered {@code @Value}
 * lookups (including the {@code sociall_app.expireTimeRefresh} typo) with a
 * single {@code social-app.*} namespace that fails fast on boot with a
 * descriptive error when a required value is missing.
 */
@Validated
@ConfigurationProperties(prefix = "social-app")
@Data
public class SocialAppProperties {

  @Valid
  private final Jwt jwt = new Jwt();

  @Valid
  private final Cloudinary cloudinary = new Cloudinary();

  @Valid
  private final Cors cors = new Cors();

  @Valid
  private final Cookie cookie = new Cookie();

  @Data
  public static class Jwt {
    // HS512 requires a key of at least 512 bits; a 64-character (ASCII)
    // secret gives exactly that. jjwt's Keys.hmacShaKeyFor throws
    // WeakKeyException for anything shorter, so this is checked here
    // instead of failing deep inside token generation on first use.
    @NotBlank
    @Size(min = 64, message = "must be at least 64 characters (512 bits) for HS512")
    private String secret;
    @NotBlank
    @Size(min = 64, message = "must be at least 64 characters (512 bits) for HS512")
    private String refreshSecret;
    @Positive
    private long accessTtl;
    @Positive
    private long refreshTtl;
  }

  @Data
  public static class Cloudinary {
    @NotBlank
    private String cloudName;
    @NotBlank
    private String cloudApiKey;
    @NotBlank
    private String cloudSecretKey;
  }

  @Data
  public static class Cors {
    @NotEmpty
    private List<String> allowedOrigins;
  }

  @Data
  public static class Cookie {
    private boolean secure = true;
  }
}
