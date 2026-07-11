package com.example.social_be.service;

import com.example.social_be.exception.ForbiddenException;
import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.PostCollection;
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

  public PostCollection upload(MultipartFile multipartFile, PostCollection postRequest) throws IOException {
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
    return postRepository.save(_post);
  }

  public PostCollection edit(MultipartFile multipartFile, PostCollection postCollection, String id, String cloudId)
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
    return postRepository.save(_post);
  }

  public Page<PostCollection> getUserFollowing(Collection<String> userIds, Pageable pageable) {
    return postRepository.findByUserIdIn(userIds, pageable);
  }

  public Page<PostCollection> getAllPost(Pageable pageable) {
    return postRepository.findAll(pageable);
  }

  public Page<PostCollection> getAllPostUser(String id, Pageable pageable) {
    return postRepository.findAllByUserId(id, pageable);
  }

  public PostCollection getPostById(String id) {
    return postRepository.findPostCollectionById(id);
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
