package com.example.social_be.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponseSocket {
  private CommentResponse comment;
  private Long amountComment;
}
