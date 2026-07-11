package com.example.social_be.model.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
  private String password;
  private String displayName;
  private String avatar;
  @Size(max = 500)
  private String about;
}
