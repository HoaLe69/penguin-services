package com.example.social_be.controller;

import com.example.social_be.repository.CommentRepository;
import com.example.social_be.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

  @Mock
  private CommentRepository commentRepository;
  @Mock
  private PostRepository postRepository;
  @InjectMocks
  private CommentController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
        .build();
  }

  @Test
  void getAllComment_usesConfiguredDefaultPageSize_sortedByCreateAtDescending() throws Exception {
    when(commentRepository.findAllByPostId(eq("post-1"), any(Pageable.class))).thenReturn(Page.empty());

    mockMvc.perform(get("/api/comment/post-1"))
        .andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(commentRepository).findAllByPostId(eq("post-1"), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getSort().getOrderFor("createAt")).isNotNull();
    assertThat(pageableCaptor.getValue().getSort().getOrderFor("createAt").isDescending()).isTrue();
  }

  @Test
  void getAllComment_acceptsExplicitPageAndSize() throws Exception {
    when(commentRepository.findAllByPostId(eq("post-1"), any(Pageable.class))).thenReturn(Page.empty());

    mockMvc.perform(get("/api/comment/post-1").param("page", "2").param("size", "5"))
        .andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(commentRepository).findAllByPostId(eq("post-1"), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
  }
}
