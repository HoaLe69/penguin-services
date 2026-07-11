package com.example.social_be.controller;

import com.example.social_be.model.request.ConversationRequest;
import com.example.social_be.model.request.UpdateLastestMessageRequest;
import com.example.social_be.model.response.MessageResponse;
import com.example.social_be.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {
  @Autowired
  private ConversationService conversationService;

  @PostMapping("/create")
  public ResponseEntity<?> createConversation(@Valid @RequestBody ConversationRequest conversationRequest) {
    return ResponseEntity.ok(conversationService.createConversation(conversationRequest.getMember()));
  }

  @GetMapping("/find/{senderId}/{receiveId}")
  public ResponseEntity<?> findConversation(@PathVariable String senderId, @PathVariable String receiveId) {
    return ResponseEntity.ok(conversationService.findConversation(senderId, receiveId));
  }

  @GetMapping("/all/{id}")
  public ResponseEntity<?> getAllRoomConversation(@PathVariable String id) {
    return ResponseEntity.ok(conversationService.getAllRoomConversation(id));
  }

  @PatchMapping("/update/lastestMessage/{id}")
  public ResponseEntity<?> updateLastestMessage(@PathVariable String id,
      @Valid @RequestBody UpdateLastestMessageRequest request) {
    conversationService.updateLastestMessage(id, request.getLastestMessage());
    return ResponseEntity.ok(new MessageResponse("ok"));
  }
}
