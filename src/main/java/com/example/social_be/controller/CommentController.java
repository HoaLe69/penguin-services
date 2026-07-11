package com.example.social_be.controller;

import com.example.social_be.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comment")
public class CommentController {

  @Autowired
  private CommentService commentService;

  @GetMapping("/{id}")
  public ResponseEntity<?> getAllComment(@PathVariable String id,
      @PageableDefault(sort = "createAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(commentService.getAllComment(id, pageable));
  }
}
