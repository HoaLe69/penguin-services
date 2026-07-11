package com.example.social_be.repository;

import com.example.social_be.model.collection.ConversationCollection;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ConversationRepository extends MongoRepository<ConversationCollection, String> {
  ConversationCollection findConversationCollectionById(String id);

  // Single query for every room a user participates in, instead of loading
  // every room and filtering in memory.
  List<ConversationCollection> findByMemberContaining(String userId);
}
