package com.example.social_be.model.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthLoginRequest {
  @NotBlank
  private String userName;
  @NotBlank
  private String password;
}
