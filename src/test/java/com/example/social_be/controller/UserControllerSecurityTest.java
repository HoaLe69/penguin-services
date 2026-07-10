package com.example.social_be.controller;

import com.example.social_be.exception.GlobalExceptionHandler;
import com.example.social_be.model.collection.UserCollection;
import com.example.social_be.model.custom.CustomUserDetail;
import com.example.social_be.repository.CommentRepository;
import com.example.social_be.repository.PostRepository;
import com.example.social_be.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerSecurityTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder encoder;
  @Mock
  private PostRepository postRepository;
  @Mock
  private CommentRepository commentRepository;
  @InjectMocks
  private UserController controller;

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

  @Test
  void update_anotherUsersProfile_is_forbidden() throws Exception {
    mockMvc.perform(patch("/api/user/update/" + SOMEONE_ELSE)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"about\":\"hacked\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));

    verify(userRepository, never()).save(any(UserCollection.class));
  }

  @Test
  void update_ownProfile_succeeds() throws Exception {
    UserCollection me = new UserCollection();
    me.setId(ME);
    when(userRepository.findUserCollectionById(ME)).thenReturn(me);
    when(userRepository.save(any(UserCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    mockMvc.perform(patch("/api/user/update/" + ME)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"about\":\"my new bio\"}"))
        .andExpect(status().isOk());

    verify(userRepository).save(any(UserCollection.class));
  }

  @Test
  void delete_anotherUser_is_forbidden() throws Exception {
    mockMvc.perform(delete("/api/user/delete/" + SOMEONE_ELSE))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));

    verify(userRepository, never()).deleteById(anyString());
  }

  @Test
  void delete_self_succeeds() throws Exception {
    mockMvc.perform(delete("/api/user/delete/" + ME))
        .andExpect(status().isOk());

    verify(userRepository).deleteById(ME);
  }
}
