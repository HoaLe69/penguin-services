package com.example.social_be.controller;

import com.example.social_be.model.collection.PostCollection;
import com.example.social_be.model.request.RequestList;
import com.example.social_be.model.response.MessageResponse;
import com.example.social_be.exception.ForbiddenException;
import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.repository.CommentRepository;
import com.example.social_be.repository.PostRepository;
import com.example.social_be.security.SecurityUtils;
import com.example.social_be.service.CloudinaryServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/post")
public class PostController {
  @Autowired
  private CloudinaryServiceImpl cloudinary;
  @Autowired
  private PostRepository postRepository;
  @Autowired
  private CommentRepository commentRepository;

  // create post
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> upload(@RequestPart(value = "file", required = false) MultipartFile multipartFile,
      @RequestPart("formData") PostCollection postRequest) throws IOException {
    // if (multipartFile == null) {
    // PostCollection _post = new PostCollection();
    // _post.setUserId(postRequest.getUserId());
    // _post.setDisplayName(postRequest.getDisplayName());
    // _post.setPhotoUrl(postRequest.getPhotoUrl());
    // _post.setLike(new ArrayList<>());
    // _post.setDescription(postRequest.getDescription());
    // _post.setComments(0);
    // return ResponseEntity.ok(postRepository.save(_post));
    // } else {
    Map<String, String> resource_url = cloudinary.uploadFile(multipartFile);
    String fileType = postRequest.getFileType();
    if (fileType.equals("image")) {
      postRequest.setThumbnail(resource_url.get("url"));
      postRequest.setVideoSrc(null);
    } else {
      postRequest.setVideoSrc(resource_url.get("url"));
      postRequest.setThumbnail(null);
    }
    postRequest.setCloudinaryId(resource_url.get("public_id"));
    PostCollection _post = new PostCollection(postRequest.getUserId(), postRequest.getPhotoUrl(),
        postRequest.getDisplayName(), postRequest.getTag(), postRequest.getThumbnail(), postRequest.getCloudinaryId(),
        postRequest.getDescription(), postRequest.getFileType(), postRequest.getVideoSrc());
    return ResponseEntity.ok(postRepository.save(_post));
  }

  @PatchMapping(value = "/edit/{id}/{cloudId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> edit(@RequestPart(value = "file", required = false) MultipartFile multipartFile,
      @RequestPart("formData") PostCollection postCollection, @PathVariable String id, @PathVariable String cloudId)
      throws IOException {
    PostCollection _post = postRepository.findPostCollectionById(id);
    if (_post == null)
      throw ResourceNotFoundException.of("Post", id);
    if (!_post.getUserId().equals(SecurityUtils.currentUserId()))
      throw new ForbiddenException("You are not the owner of this post");
    if (multipartFile != null) {
      cloudinary.destroy(cloudId, postCollection.getFileType());
      Map<String, String> thumbnail = cloudinary.uploadFile(multipartFile);
      _post.setThumbnail(thumbnail.get("url"));
      _post.setCloudinaryId(thumbnail.get("public_id"));
    }
    _post.setDescription(postCollection.getDescription());
    _post.setTag(postCollection.getTag());
    return ResponseEntity.ok(postRepository.save(_post));
  }

  @PostMapping("/all-post-user-following")
  public ResponseEntity<?> getUserFollowing(@Valid @RequestBody RequestList list) {
    List<PostCollection> listPost = new ArrayList<>();
    for (String userId : list.getList()) {
      listPost.addAll(postRepository.findAllByUserId(userId));
    }
    return ResponseEntity.ok(listPost);
  }

  // get all post
  @GetMapping("/all-post")
  public ResponseEntity<?> getAllPost(@RequestParam("page") String page) {
    Pageable pageable = PageRequest.of(Integer.parseInt(page), 2, Sort.by("createAt").descending());
    return ResponseEntity.ok(postRepository.findAll(pageable));
  }

  // get all post of user
  @GetMapping("/all-post-user/{id}")
  public ResponseEntity<?> getAllPostUser(@PathVariable String id) {
    return ResponseEntity.ok(postRepository.findAllByUserId(id));
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getPostById(@PathVariable String id) {
    return ResponseEntity.ok(postRepository.findPostCollectionById(id));
  }

  // delete post
  @DeleteMapping("/delete/{id}/{cloudId}/{fileType}")
  public ResponseEntity<?> deletePost(@PathVariable String id, @PathVariable String cloudId,
      @PathVariable String fileType) throws IOException {
    PostCollection post = postRepository.findPostCollectionById(id);
    if (post == null)
      throw ResourceNotFoundException.of("Post", id);
    if (!post.getUserId().equals(SecurityUtils.currentUserId()))
      throw new ForbiddenException("You are not the owner of this post");
    cloudinary.destroy(cloudId, fileType);
    commentRepository.deleteAllByPostId(id);
    postRepository.deleteById(id);
    return ResponseEntity.ok(new MessageResponse("Delete Successfully"));
  }

  @PatchMapping("/react/{id}")
  public ResponseEntity<?> reactPost(@PathVariable String id) {
    String userId = SecurityUtils.currentUserId();
    PostCollection post = postRepository.findPostCollectionById(id);
    if (post != null) {
      List<String> likes = post.getLike();
      if (likes.contains(userId)) {
        likes.remove(userId);
      } else
        likes.add(userId);
      post.setLike(likes);
      postRepository.save(post);
      return ResponseEntity.ok("ok");
    }
    return ResponseEntity.badRequest().body("This post removed by owner");
  }

}
