package com.example.social_be.service;

import com.example.social_be.model.collection.MessageCollection;
import com.example.social_be.model.request.MessageRequestSocket;
import com.example.social_be.model.response.ChatMessageResponse;
import com.example.social_be.model.response.MessageResponse;
import com.example.social_be.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {
  @Autowired
  private MessageRepository messageRepository;

  public List<ChatMessageResponse> getAllMessages(String conversationId) {
    return messageRepository.findAllByConversationId(conversationId).stream()
        .map(ChatMessageResponse::new)
        .collect(Collectors.toList());
  }

  // Returns null when the message no longer exists so the controller can
  // reproduce the existing 400 "message not found" response instead of a 404.
  public MessageCollection recallMessage(String id) {
    MessageCollection message = messageRepository.findMessageCollectionById(id);
    if (message == null)
      return null;
    message.setContent(null);
    return messageRepository.save(message);
  }

  public Object handleSocketMessage(String conversationId, MessageRequestSocket message) {
    if (message.getDeleteMessage() == 1) {
      return new MessageResponse(message.getId());
    }
    MessageCollection savedMessage = new MessageCollection(message.getContent(), message.getUserId(), conversationId);
    return new ChatMessageResponse(messageRepository.save(savedMessage));
  }
}
