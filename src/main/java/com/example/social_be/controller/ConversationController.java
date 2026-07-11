package com.example.social_be.controller;

import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.ConversationCollection;
import com.example.social_be.model.request.ConversationRequest;
import com.example.social_be.model.response.MessageResponse;
import com.example.social_be.repository.ConversationRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {
  @Autowired
  private ConversationRepository conversationRepository;
  @Autowired
  private MongoTemplate mongoTemplate;

  @PostMapping("/create")
  public ResponseEntity<?> createConversation(@Valid @RequestBody ConversationRequest conversationRequest) {
    ConversationCollection conversationCollection = new ConversationCollection(conversationRequest.getMember());
    ConversationCollection savedConversation = conversationRepository.save(conversationCollection);
    return ResponseEntity.ok(savedConversation);
  }

  @GetMapping("/find/{senderId}/{receiveId}")
  public ResponseEntity<?> findConversation(@PathVariable String senderId, @PathVariable String receiveId) {
    Query query = new Query();
    query.addCriteria(Criteria.where("member").all(senderId, receiveId));
    ConversationCollection result = mongoTemplate.findOne(query, ConversationCollection.class, "room");
    return ResponseEntity.ok(result);
  }

  @GetMapping("/all/{id}")
  public ResponseEntity<?> getAllRoomConversation(@PathVariable String id) {
    List<ConversationCollection> rooms = conversationRepository.findByMemberContaining(id);
    return ResponseEntity.ok(rooms);
  }

  @PatchMapping("/update/lastestMessage/{id}")
  public ResponseEntity<?> updateLastestMessage(@PathVariable String id,
      @RequestBody ConversationCollection conversation) {
    ConversationCollection conversationCollection = conversationRepository.findConversationCollectionById(id);
    if (conversationCollection == null)
      throw ResourceNotFoundException.of("Conversation", id);
    conversationCollection.setLastestMessage(conversation.getLastestMessage());
    conversationRepository.save(conversationCollection);
    return ResponseEntity.ok(new MessageResponse("ok"));
  }
}
