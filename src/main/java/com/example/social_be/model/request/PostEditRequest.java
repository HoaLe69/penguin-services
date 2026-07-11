package com.example.social_be.model.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Deliberately unvalidated, matching the pre-existing PostCollection-as-request
// behavior this DTO replaces - adding new validation here is a separate concern.
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostEditRequest {
  private String description;
  private String tag;
  private String fileType;
}
