package com.example.social_be.service;

import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.ConversationCollection;
import com.example.social_be.model.response.ConversationResponse;
import com.example.social_be.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {
  @Autowired
  private ConversationRepository conversationRepository;
  @Autowired
  private MongoTemplate mongoTemplate;

  public ConversationResponse createConversation(List<String> member) {
    return new ConversationResponse(conversationRepository.save(new ConversationCollection(member)));
  }

  public ConversationResponse findConversation(String senderId, String receiveId) {
    Query query = new Query();
    query.addCriteria(Criteria.where("member").all(senderId, receiveId));
    ConversationCollection result = mongoTemplate.findOne(query, ConversationCollection.class, "room");
    return result == null ? null : new ConversationResponse(result);
  }

  public List<ConversationResponse> getAllRoomConversation(String userId) {
    return conversationRepository.findByMemberContaining(userId).stream()
        .map(ConversationResponse::new)
        .collect(Collectors.toList());
  }

  public ConversationResponse updateLastestMessage(String id, String lastestMessage) {
    ConversationCollection conversation = conversationRepository.findConversationCollectionById(id);
    if (conversation == null)
      throw ResourceNotFoundException.of("Conversation", id);
    conversation.setLastestMessage(lastestMessage);
    return new ConversationResponse(conversationRepository.save(conversation));
  }
}
