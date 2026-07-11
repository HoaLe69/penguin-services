package com.example.social_be.service;

import com.example.social_be.exception.ForbiddenException;
import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.PostCollection;
import com.example.social_be.model.custom.CustomUserDetail;
import com.example.social_be.model.request.PostEditRequest;
import com.example.social_be.model.response.PostResponse;
import com.example.social_be.repository.CommentRepository;
import com.example.social_be.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

  @Mock
  private CloudinaryServiceImpl cloudinary;
  @Mock
  private PostRepository postRepository;
  @Mock
  private CommentRepository commentRepository;
  @InjectMocks
  private PostService postService;

  private static final String ME = "user-1";
  private static final String SOMEONE_ELSE = "user-2";

  @BeforeEach
  void setUp() {
    var principal = new CustomUserDetail(ME, "me", "me@x", "Me", null, null,
        List.of(), List.of(), List.of(), "pw");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private PostCollection postOwnedBy(String ownerId) {
    PostCollection p = new PostCollection();
    p.setId("p1");
    p.setUserId(ownerId);
    p.setLike(new ArrayList<>());
    return p;
  }

  @Test
  void getPostById_delegatesToRepository() {
    PostCollection post = postOwnedBy(ME);
    when(postRepository.findPostCollectionById("p1")).thenReturn(post);

    assertThat(postService.getPostById("p1")).isEqualTo(new PostResponse(post));
  }

  @Test
  void getAllPost_delegatesToRepository() {
    Pageable pageable = Pageable.unpaged();
    PostCollection post = postOwnedBy(ME);
    Page<PostCollection> page = new PageImpl<>(List.of(post));
    when(postRepository.findAll(pageable)).thenReturn(page);

    assertThat(postService.getAllPost(pageable).getContent())
        .containsExactly(new PostResponse(post));
  }

  @Test
  void getAllPostUser_delegatesToRepository() {
    Pageable pageable = Pageable.unpaged();
    PostCollection post = postOwnedBy(ME);
    Page<PostCollection> page = new PageImpl<>(List.of(post));
    when(postRepository.findAllByUserId(ME, pageable)).thenReturn(page);

    assertThat(postService.getAllPostUser(ME, pageable).getContent())
        .containsExactly(new PostResponse(post));
  }

  @Test
  void getUserFollowing_delegatesToRepository() {
    Pageable pageable = Pageable.unpaged();
    List<String> ids = List.of("a", "b");
    PostCollection post = postOwnedBy(ME);
    Page<PostCollection> page = new PageImpl<>(List.of(post));
    when(postRepository.findByUserIdIn(ids, pageable)).thenReturn(page);

    assertThat(postService.getUserFollowing(ids, pageable).getContent())
        .containsExactly(new PostResponse(post));
  }

  @Test
  void deletePost_notFound_throwsResourceNotFound() {
    when(postRepository.findPostCollectionById("missing")).thenReturn(null);

    assertThatThrownBy(() -> postService.deletePost("missing", "cloud1", "image"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deletePost_notOwner_throwsForbidden() {
    when(postRepository.findPostCollectionById("p1")).thenReturn(postOwnedBy(SOMEONE_ELSE));

    assertThatThrownBy(() -> postService.deletePost("p1", "cloud1", "image"))
        .isInstanceOf(ForbiddenException.class);

    verify(postRepository, never()).deleteById(any());
  }

  @Test
  void deletePost_owner_deletesPostAndComments() throws Exception {
    when(postRepository.findPostCollectionById("p1")).thenReturn(postOwnedBy(ME));

    postService.deletePost("p1", "cloud1", "image");

    verify(cloudinary).destroy("cloud1", "image");
    verify(commentRepository).deleteAllByPostId("p1");
    verify(postRepository).deleteById("p1");
  }

  @Test
  void edit_notFound_throwsResourceNotFound() {
    when(postRepository.findPostCollectionById("missing")).thenReturn(null);

    assertThatThrownBy(() -> postService.edit(null, new PostEditRequest(), "missing", "cloud1"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void edit_notOwner_throwsForbidden() {
    when(postRepository.findPostCollectionById("p1")).thenReturn(postOwnedBy(SOMEONE_ELSE));

    assertThatThrownBy(() -> postService.edit(null, new PostEditRequest(), "p1", "cloud1"))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void edit_owner_updatesDescriptionAndTag() throws Exception {
    PostCollection stored = postOwnedBy(ME);
    when(postRepository.findPostCollectionById("p1")).thenReturn(stored);
    when(postRepository.save(any(PostCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    PostEditRequest edits = new PostEditRequest();
    edits.setDescription("new desc");
    edits.setTag("new tag");

    PostResponse result = postService.edit(null, edits, "p1", "cloud1");

    assertThat(result.getDescription()).isEqualTo("new desc");
    assertThat(result.getTag()).isEqualTo("new tag");
    verify(cloudinary, never()).destroy(any(), any());
  }

  @Test
  void reactPost_postMissing_returnsNull() {
    when(postRepository.findPostCollectionById("missing")).thenReturn(null);

    assertThat(postService.reactPost("missing")).isNull();
    verify(postRepository, never()).save(any());
  }

  @Test
  void reactPost_addsLike_whenNotAlreadyLiked() {
    PostCollection post = postOwnedBy(SOMEONE_ELSE);
    when(postRepository.findPostCollectionById("p1")).thenReturn(post);
    when(postRepository.save(any(PostCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    String result = postService.reactPost("p1");

    assertThat(result).isEqualTo("ok");
    assertThat(post.getLike()).containsExactly(ME);
  }

  @Test
  void reactPost_removesLike_whenAlreadyLiked() {
    PostCollection post = postOwnedBy(SOMEONE_ELSE);
    post.getLike().add(ME);
    when(postRepository.findPostCollectionById("p1")).thenReturn(post);
    when(postRepository.save(any(PostCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    String result = postService.reactPost("p1");

    assertThat(result).isEqualTo("ok");
    assertThat(post.getLike()).isEmpty();
  }
}
