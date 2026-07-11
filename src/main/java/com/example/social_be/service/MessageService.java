package com.example.social_be.service;

import com.example.social_be.model.collection.MessageCollection;
import com.example.social_be.model.request.MessageRequestSocket;
import com.example.social_be.model.response.MessageResponse;
import com.example.social_be.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageService {
  @Autowired
  private MessageRepository messageRepository;

  public Object handleSocketMessage(String conversationId, MessageRequestSocket message) {
    if (message.getDeleteMessage() == 1) {
      return new MessageResponse(message.getId());
    }
    MessageCollection savedMessage = new MessageCollection(message.getContent(), message.getUserId(), conversationId);
    return messageRepository.save(savedMessage);
  }
}
