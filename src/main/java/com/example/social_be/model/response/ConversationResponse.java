package com.example.social_be.model.response;

import com.example.social_be.model.collection.ConversationCollection;
import lombok.Data;

import java.util.List;

@Data
public class ConversationResponse {
  private String id;
  private List<String> member;
  private String lastestMessage;
  private String createAt;

  public ConversationResponse(ConversationCollection conversation) {
    this.id = conversation.getId();
    this.member = conversation.getMember();
    this.lastestMessage = conversation.getLastestMessage();
    this.createAt = conversation.getCreateAt();
  }
}
