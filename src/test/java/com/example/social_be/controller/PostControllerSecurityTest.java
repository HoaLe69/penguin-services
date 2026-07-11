package com.example.social_be.controller;

import com.example.social_be.exception.ForbiddenException;
import com.example.social_be.exception.GlobalExceptionHandler;
import com.example.social_be.model.collection.PostCollection;
import com.example.social_be.model.custom.CustomUserDetail;
import com.example.social_be.repository.CommentRepository;
import com.example.social_be.repository.PostRepository;
import com.example.social_be.service.CloudinaryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PostControllerSecurityTest {

  @Mock
  private CloudinaryServiceImpl cloudinary;
  @Mock
  private PostRepository postRepository;
  @Mock
  private CommentRepository commentRepository;
  @InjectMocks
  private PostController controller;

  private MockMvc mockMvc;

  private static final String ME = "user-1";
  private static final String SOMEONE_ELSE = "user-2";

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    var principal = new CustomUserDetail(ME, "me", "me@x", "Me", null, null,
        List.of(), List.of(), List.of(), "pw");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
  }

  @AfterEach
  void clear() {
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
  void delete_someoneElsesPost_is_forbidden() throws Exception {
    when(postRepository.findPostCollectionById("p1")).thenReturn(postOwnedBy(SOMEONE_ELSE));

    mockMvc.perform(delete("/api/post/delete/p1/cloud1/image"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));

    verify(cloudinary, never()).destroy(anyString(), anyString());
    verify(postRepository, never()).deleteById(anyString());
  }

  @Test
  void delete_ownPost_succeeds() throws Exception {
    when(postRepository.findPostCollectionById("p1")).thenReturn(postOwnedBy(ME));

    mockMvc.perform(delete("/api/post/delete/p1/cloud1/image"))
        .andExpect(status().isOk());

    verify(postRepository).deleteById("p1");
  }

  @Test
  void edit_someoneElsesPost_throws_forbidden() {
    when(postRepository.findPostCollectionById("p1")).thenReturn(postOwnedBy(SOMEONE_ELSE));

    assertThatThrownBy(() -> controller.edit(null, new PostCollection(), "p1", "cloud1"))
        .isInstanceOf(ForbiddenException.class);
  }

  @Test
  void react_uses_principal_id_not_a_path_variable() throws Exception {
    PostCollection post = postOwnedBy(SOMEONE_ELSE); // reacting to another user's post is fine
    when(postRepository.findPostCollectionById("p1")).thenReturn(post);
    when(postRepository.save(any(PostCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    mockMvc.perform(patch("/api/post/react/p1"))
        .andExpect(status().isOk());

    ArgumentCaptor<PostCollection> captor = ArgumentCaptor.forClass(PostCollection.class);
    verify(postRepository).save(captor.capture());
    assertThat(captor.getValue().getLike()).containsExactly(ME);
  }

  @Test
  void getUserFollowing_issuesSingleBatchQuery_notOnePerUser() throws Exception {
    when(postRepository.findByUserIdIn(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(postOwnedBy("user-3"))));

    mockMvc.perform(post("/api/post/all-post-user-following")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"list\":[\"user-2\",\"user-3\",\"user-4\"]}"))
        .andExpect(status().isOk());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Collection<String>> idsCaptor = ArgumentCaptor.forClass(Collection.class);
    verify(postRepository).findByUserIdIn(idsCaptor.capture(), any(Pageable.class));
    assertThat(idsCaptor.getValue()).containsExactly("user-2", "user-3", "user-4");
    verify(postRepository, never()).findAllByUserId(anyString());
  }
}
