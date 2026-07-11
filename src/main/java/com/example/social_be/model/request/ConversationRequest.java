package com.example.social_be.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConversationRequest {
  // ConversationController assumes exactly two members (member.get(0)/get(1)
  // in getAllRoomConversation), so this is enforced here rather than left to
  // crash later with a NullPointerException/IndexOutOfBoundsException.
  @NotNull
  @Size(min = 2, max = 2)
  private List<String> member;
  private String lastestMessage;
}
