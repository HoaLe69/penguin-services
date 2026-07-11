package com.example.social_be.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthSignUpRequest {
  @NotBlank
  @Size(min = 3, max = 30)
  private String userName;
  @NotBlank
  @Email
  private String email;
  // BCrypt silently truncates beyond 72 bytes, so cap the input there.
  @NotBlank
  @Size(min = 8, max = 72)
  private String password;
}
