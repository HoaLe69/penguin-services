package com.example.social_be.controller;

import com.example.social_be.model.request.CommentRequestSocket;
import com.example.social_be.model.request.MessageRequestSocket;
import com.example.social_be.service.CommentService;
import com.example.social_be.service.MessageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;

/**
 * Thin STOMP transport layer: handlers delegate persistence/business logic
 * to CommentService/MessageService and return plain payloads (no
 * ResponseEntity - that's an HTTP construct, not a STOMP one). Errors are
 * routed to the sending user's /queue/errors instead of being swallowed.
 */
@Controller
public class WebSocketController {
  private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

  @Autowired
  private MessageService messageService;
  @Autowired
  private CommentService commentService;

  // Handles messages from /app/messages/{id}; broadcasts the result to
  // /topic/messages/{id}.
  @MessageMapping("/messages/{id}")
  @SendTo("/topic/messages/{id}")
  public Object handleMessage(@DestinationVariable String id, MessageRequestSocket message) {
    return messageService.handleSocketMessage(id, message);
  }

  // Handles comments from /app/comments/{id}; broadcasts the result to
  // /topic/comments/{id}.
  @MessageMapping("/comments/{id}")
  @SendTo("/topic/comments/{id}")
  public Object handleComment(@DestinationVariable String id, CommentRequestSocket commentRequest) {
    return commentService.handleSocketComment(id, commentRequest);
  }

  @MessageExceptionHandler
  @SendToUser("/queue/errors")
  public Map<String, String> handleException(Exception ex) {
    log.error("WebSocket message handling failed", ex);
    String message = ex.getMessage() != null ? ex.getMessage() : "Something went wrong";
    return Map.of("error", message);
  }
}
