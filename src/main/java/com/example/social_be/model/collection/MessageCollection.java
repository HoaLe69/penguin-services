package com.example.social_be.model.collection;

import com.example.social_be.util.Utilties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

// Backs MessageRepository.findAllByConversationId, so message-list lookups
// don't collection-scan; also orders by createAt for when the currently
// unused sorted/paged overload gets wired up.
@CompoundIndex(name = "conversationId_createAt_idx", def = "{'conversationId': 1, 'createAt': -1}")
@Document(value = "messages")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageCollection {
  private String userId;
  private String content;
  private MessageCollection reply;
  private String id;
  private String conversationId;
  private Date createAt;

  public MessageCollection(String content, String userId, String conversationId) {
    this.content = content;
    this.userId = userId;
    this.conversationId = conversationId;
    this.createAt = new Date();
  }
}
