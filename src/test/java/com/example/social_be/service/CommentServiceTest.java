package com.example.social_be.service;

import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.CommentCollection;
import com.example.social_be.model.collection.PostCollection;
import com.example.social_be.model.request.CommentRequestSocket;
import com.example.social_be.model.response.CommentResponseSocket;
import com.example.social_be.repository.CommentRepository;
import com.example.social_be.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock
  private CommentRepository commentRepository;
  @Mock
  private PostRepository postRepository;
  @InjectMocks
  private CommentService commentService;

  private CommentRequestSocket createRequest(String postId, String content) {
    CommentRequestSocket request = new CommentRequestSocket(
        null, null, null, null, null, null, null, null, null, null, null);
    request.setPostId(postId);
    request.setContent(content);
    request.setAvatar("avatar");
    request.setDisplayName("Alice");
    request.setUserId("user-1");
    request.setAction("CREATE");
    return request;
  }

  @Test
  void getAllComment_delegatesToRepository() {
    Pageable pageable = Pageable.unpaged();
    Page<CommentCollection> page = Page.empty();
    when(commentRepository.findAllByPostId(eq("post-1"), eq(pageable))).thenReturn(page);

    assertThat(commentService.getAllComment("post-1", pageable)).isSameAs(page);
  }

  @Test
  void postNotFound_throwsResourceNotFound() {
    when(postRepository.findPostCollectionById("missing-post")).thenReturn(null);
    CommentRequestSocket request = createRequest("missing-post", "hi");

    assertThatThrownBy(() -> commentService.handleSocketComment("missing-post", request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void create_savesComment_andIncrementsPostCounter() {
    PostCollection post = new PostCollection();
    post.setId("p1");
    post.setComments(2);
    when(postRepository.findPostCollectionById("p1")).thenReturn(post);
    when(commentRepository.save(any(CommentCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    CommentRequestSocket request = createRequest("p1", "hi there");

    Object result = commentService.handleSocketComment("p1", request);

    assertThat(result).isInstanceOf(CommentResponseSocket.class);
    CommentResponseSocket response = (CommentResponseSocket) result;
    assertThat(response.getAmountComment()).isEqualTo(3L);
    assertThat(response.getComment().getContent()).isEqualTo("hi there");
    assertThat(post.getComments()).isEqualTo(3L);
  }

  @Test
  void delete_removesComment_andDecrementsPostCounter() {
    PostCollection post = new PostCollection();
    post.setId("p1");
    post.setComments(3);
    when(postRepository.findPostCollectionById("p1")).thenReturn(post);
    CommentCollection deleted = new CommentCollection();
    deleted.setId("c1");
    when(commentRepository.deleteCommentCollectionById("c1")).thenReturn(deleted);

    CommentRequestSocket request = new CommentRequestSocket(
        "c1", null, null, null, null, null, null, null, null, null, "DELETE");

    Object result = commentService.handleSocketComment("p1", request);

    assertThat(result).isInstanceOf(Map.class);
    @SuppressWarnings("unchecked")
    Map<String, String> response = (Map<String, String>) result;
    assertThat(response).containsEntry("id", "c1").containsEntry("action", "DELETE").containsEntry("amountComment", "2");
    assertThat(post.getComments()).isEqualTo(2L);
  }
}
