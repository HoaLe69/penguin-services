package com.example.social_be.service;

import com.example.social_be.exception.ForbiddenException;
import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.PostCollection;
import com.example.social_be.model.request.PostEditRequest;
import com.example.social_be.model.request.PostUploadRequest;
import com.example.social_be.model.response.PostResponse;
import com.example.social_be.repository.CommentRepository;
import com.example.social_be.repository.PostRepository;
import com.example.social_be.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class PostService {
  @Autowired
  private CloudinaryServiceImpl cloudinary;
  @Autowired
  private PostRepository postRepository;
  @Autowired
  private CommentRepository commentRepository;

  public PostResponse upload(MultipartFile multipartFile, PostUploadRequest postRequest) throws IOException {
    Map<String, String> resource_url = cloudinary.uploadFile(multipartFile);
    String fileType = postRequest.getFileType();
    String thumbnail;
    String videoSrc;
    if (fileType.equals("image")) {
      thumbnail = resource_url.get("url");
      videoSrc = null;
    } else {
      videoSrc = resource_url.get("url");
      thumbnail = null;
    }
    String cloudinaryId = resource_url.get("public_id");
    PostCollection _post = new PostCollection(postRequest.getUserId(), postRequest.getPhotoUrl(),
        postRequest.getDisplayName(), postRequest.getTag(), thumbnail, cloudinaryId,
        postRequest.getDescription(), postRequest.getFileType(), videoSrc);
    return new PostResponse(postRepository.save(_post));
  }

  public PostResponse edit(MultipartFile multipartFile, PostEditRequest postEditRequest, String id, String cloudId)
      throws IOException {
    PostCollection _post = postRepository.findPostCollectionById(id);
    if (_post == null)
      throw ResourceNotFoundException.of("Post", id);
    if (!_post.getUserId().equals(SecurityUtils.currentUserId()))
      throw new ForbiddenException("You are not the owner of this post");
    if (multipartFile != null) {
      cloudinary.destroy(cloudId, postEditRequest.getFileType());
      Map<String, String> thumbnail = cloudinary.uploadFile(multipartFile);
      _post.setThumbnail(thumbnail.get("url"));
      _post.setCloudinaryId(thumbnail.get("public_id"));
    }
    _post.setDescription(postEditRequest.getDescription());
    _post.setTag(postEditRequest.getTag());
    return new PostResponse(postRepository.save(_post));
  }

  public Page<PostResponse> getUserFollowing(Collection<String> userIds, Pageable pageable) {
    return postRepository.findByUserIdIn(userIds, pageable).map(PostResponse::new);
  }

  public Page<PostResponse> getAllPost(Pageable pageable) {
    return postRepository.findAll(pageable).map(PostResponse::new);
  }

  public Page<PostResponse> getAllPostUser(String id, Pageable pageable) {
    return postRepository.findAllByUserId(id, pageable).map(PostResponse::new);
  }

  public PostResponse getPostById(String id) {
    PostCollection post = postRepository.findPostCollectionById(id);
    return post == null ? null : new PostResponse(post);
  }

  public void deletePost(String id, String cloudId, String fileType) throws IOException {
    PostCollection post = postRepository.findPostCollectionById(id);
    if (post == null)
      throw ResourceNotFoundException.of("Post", id);
    if (!post.getUserId().equals(SecurityUtils.currentUserId()))
      throw new ForbiddenException("You are not the owner of this post");
    cloudinary.destroy(cloudId, fileType);
    commentRepository.deleteAllByPostId(id);
    postRepository.deleteById(id);
  }

  // Returns null when the post no longer exists so the controller can
  // reproduce the existing 400 "removed by owner" response instead of a 404 -
  // preserving the pre-refactor HTTP contract for this endpoint.
  public String reactPost(String id) {
    String userId = SecurityUtils.currentUserId();
    PostCollection post = postRepository.findPostCollectionById(id);
    if (post == null)
      return null;
    List<String> likes = post.getLike();
    if (likes.contains(userId)) {
      likes.remove(userId);
    } else
      likes.add(userId);
    post.setLike(likes);
    postRepository.save(post);
    return "ok";
  }
}
