package com.example.social_be.service;

import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.ConversationCollection;
import com.example.social_be.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConversationService {
  @Autowired
  private ConversationRepository conversationRepository;
  @Autowired
  private MongoTemplate mongoTemplate;

  public ConversationCollection createConversation(List<String> member) {
    return conversationRepository.save(new ConversationCollection(member));
  }

  public ConversationCollection findConversation(String senderId, String receiveId) {
    Query query = new Query();
    query.addCriteria(Criteria.where("member").all(senderId, receiveId));
    return mongoTemplate.findOne(query, ConversationCollection.class, "room");
  }

  public List<ConversationCollection> getAllRoomConversation(String userId) {
    return conversationRepository.findByMemberContaining(userId);
  }

  public ConversationCollection updateLastestMessage(String id, String lastestMessage) {
    ConversationCollection conversation = conversationRepository.findConversationCollectionById(id);
    if (conversation == null)
      throw ResourceNotFoundException.of("Conversation", id);
    conversation.setLastestMessage(lastestMessage);
    return conversationRepository.save(conversation);
  }
}
