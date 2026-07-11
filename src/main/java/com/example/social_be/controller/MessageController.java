package com.example.social_be.controller;

import com.example.social_be.model.collection.MessageCollection;
import com.example.social_be.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/message")
public class MessageController {
  @Autowired
  private MessageService messageService;

  @GetMapping("/all/{id}")
  public ResponseEntity<?> getAllMess(@PathVariable String id) {
    return ResponseEntity.ok(messageService.getAllMessages(id));
  }

  @PatchMapping("/recall/{id}")
  public ResponseEntity<?> recallMessage(@PathVariable String id) {
    MessageCollection mess = messageService.recallMessage(id);
    if (mess != null) {
      return ResponseEntity.ok("ok");
    }
    return ResponseEntity.badRequest().body("message not found");
  }
}
