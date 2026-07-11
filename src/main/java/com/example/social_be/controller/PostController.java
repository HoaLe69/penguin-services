package com.example.social_be.controller;

import com.example.social_be.model.request.PostEditRequest;
import com.example.social_be.model.request.PostUploadRequest;
import com.example.social_be.model.request.RequestList;
import com.example.social_be.model.response.MessageResponse;
import com.example.social_be.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(value = "/api/post")
public class PostController {
  @Autowired
  private PostService postService;

  // create post
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> upload(@RequestPart(value = "file", required = false) MultipartFile multipartFile,
      @RequestPart("formData") PostUploadRequest postRequest) throws IOException {
    return ResponseEntity.ok(postService.upload(multipartFile, postRequest));
  }

  @PatchMapping(value = "/edit/{id}/{cloudId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> edit(@RequestPart(value = "file", required = false) MultipartFile multipartFile,
      @RequestPart("formData") PostEditRequest postEditRequest, @PathVariable String id, @PathVariable String cloudId)
      throws IOException {
    return ResponseEntity.ok(postService.edit(multipartFile, postEditRequest, id, cloudId));
  }

  @PostMapping("/all-post-user-following")
  public ResponseEntity<?> getUserFollowing(@Valid @RequestBody RequestList list,
      @PageableDefault(sort = "createAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(postService.getUserFollowing(list.getList(), pageable));
  }

  // get all post
  @GetMapping("/all-post")
  public ResponseEntity<?> getAllPost(
      @PageableDefault(sort = "createAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(postService.getAllPost(pageable));
  }

  // get all post of user
  @GetMapping("/all-post-user/{id}")
  public ResponseEntity<?> getAllPostUser(@PathVariable String id,
      @PageableDefault(sort = "createAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(postService.getAllPostUser(id, pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getPostById(@PathVariable String id) {
    return ResponseEntity.ok(postService.getPostById(id));
  }

  // delete post
  @DeleteMapping("/delete/{id}/{cloudId}/{fileType}")
  public ResponseEntity<?> deletePost(@PathVariable String id, @PathVariable String cloudId,
      @PathVariable String fileType) throws IOException {
    postService.deletePost(id, cloudId, fileType);
    return ResponseEntity.ok(new MessageResponse("Delete Successfully"));
  }

  @PatchMapping("/react/{id}")
  public ResponseEntity<?> reactPost(@PathVariable String id) {
    String result = postService.reactPost(id);
    if (result != null) {
      return ResponseEntity.ok(result);
    }
    return ResponseEntity.badRequest().body("This post removed by owner");
  }

}
