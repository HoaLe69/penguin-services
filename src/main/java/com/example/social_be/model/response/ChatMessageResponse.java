package com.example.social_be.model.response;

import com.example.social_be.model.collection.MessageCollection;
import lombok.Data;

import java.util.Date;

@Data
public class ChatMessageResponse {
  private String id;
  private String userId;
  private String content;
  private String conversationId;
  private Date createAt;

  public ChatMessageResponse(MessageCollection message) {
    this.id = message.getId();
    this.userId = message.getUserId();
    this.content = message.getContent();
    this.conversationId = message.getConversationId();
    this.createAt = message.getCreateAt();
  }
}
