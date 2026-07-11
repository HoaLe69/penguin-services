package com.example.social_be.service;

import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.CommentCollection;
import com.example.social_be.model.collection.PostCollection;
import com.example.social_be.model.request.CommentRequestSocket;
import com.example.social_be.model.response.CommentResponse;
import com.example.social_be.model.response.CommentResponseSocket;
import com.example.social_be.repository.CommentRepository;
import com.example.social_be.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CommentService {
  @Autowired
  private CommentRepository commentRepository;
  @Autowired
  private PostRepository postRepository;

  public Page<CommentResponse> getAllComment(String postId, Pageable pageable) {
    return commentRepository.findAllByPostId(postId, pageable).map(CommentResponse::new);
  }

  public Object handleSocketComment(String postId, CommentRequestSocket commentRequest) {
    PostCollection storedPost = postRepository.findPostCollectionById(postId);
    if (storedPost == null) {
      throw ResourceNotFoundException.of("Post", postId);
    }

    if ("DELETE".equals(commentRequest.getAction())) {
      return deleteComment(storedPost, commentRequest);
    }
    return createComment(storedPost, commentRequest);
  }

  private Map<String, String> deleteComment(PostCollection storedPost, CommentRequestSocket commentRequest) {
    CommentCollection commentDeleted = commentRepository.deleteCommentCollectionById(commentRequest.getId());
    storedPost.setComments(storedPost.getComments() - 1);
    postRepository.save(storedPost);

    Map<String, String> response = new HashMap<>();
    response.put("id", commentDeleted.getId());
    response.put("action", "DELETE");
    response.put("amountComment", String.valueOf(storedPost.getComments()));
    return response;
  }

  private CommentResponseSocket createComment(PostCollection storedPost, CommentRequestSocket commentRequest) {
    storedPost.setComments(storedPost.getComments() + 1);

    CommentCollection commentCollection = new CommentCollection();
    commentCollection.setPostId(commentRequest.getPostId());
    commentCollection.setAvatar(commentRequest.getAvatar());
    commentCollection.setDisplayName(commentRequest.getDisplayName());
    commentCollection.setUserId(commentRequest.getUserId());
    commentCollection.setReplyTo(commentRequest.getReplyTo());
    commentCollection.setContent(commentRequest.getContent());
    commentCollection.setLevel(commentRequest.getLevel());

    postRepository.save(storedPost);
    CommentCollection savedComment = commentRepository.save(commentCollection);

    CommentResponseSocket response = new CommentResponseSocket();
    response.setComment(new CommentResponse(savedComment));
    response.setAmountComment(storedPost.getComments());
    return response;
  }
}
