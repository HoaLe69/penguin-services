package com.example.social_be.controller;

import com.example.social_be.exception.GlobalExceptionHandler;
import com.example.social_be.model.collection.UserCollection;
import com.example.social_be.model.custom.CustomUserDetail;
import com.example.social_be.repository.UserRepository;
import com.example.social_be.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerSecurityTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private MongoTemplate mongoTemplate;
  @InjectMocks
  private UserService userService;

  private UserController controller;
  private MockMvc mockMvc;

  private static final String ME = "user-1";
  private static final String SOMEONE_ELSE = "user-2";

  @BeforeEach
  void setup() {
    controller = new UserController();
    ReflectionTestUtils.setField(controller, "userService", userService);

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

  @Test
  void update_response_excludes_password_and_socialId() throws Exception {
    UserCollection me = new UserCollection();
    me.setId(ME);
    me.setSocialId("google-123");
    me.setPassword("$2a$hashed");
    when(userRepository.findUserCollectionById(ME)).thenReturn(me);
    when(userRepository.save(any(UserCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    mockMvc.perform(patch("/api/user/update/" + ME)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"about\":\"my new bio\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.socialId").doesNotExist());
  }

  @Test
  void search_response_excludes_password_and_socialId() throws Exception {
    UserCollection match = new UserCollection();
    match.setId(SOMEONE_ELSE);
    match.setEmail("someone@x.com");
    match.setSocialId("google-456");
    match.setPassword("$2a$hashed");
    when(userRepository.findByLikeEmail(anyString(), any(Pageable.class))).thenReturn(List.of(match));

    mockMvc.perform(get("/api/user/search").param("email", "someone"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].password").doesNotExist())
        .andExpect(jsonPath("$[0].socialId").doesNotExist())
        .andExpect(jsonPath("$[0].email").value("someone@x.com"));
  }

  @Test
  void search_escapesRegexSpecialCharacters_andAnchorsAsPrefix() throws Exception {
    when(userRepository.findByLikeEmail(anyString(), any(Pageable.class))).thenReturn(List.of());

    mockMvc.perform(get("/api/user/search").param("email", "a.*evil(x)"))
        .andExpect(status().isOk());

    ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);
    verify(userRepository).findByLikeEmail(patternCaptor.capture(), any(Pageable.class));
    String pattern = patternCaptor.getValue();
    assertThat(pattern).startsWith("^");
    assertThat(pattern).contains("\\Qa.*evil(x)\\E");
  }

  @Test
  void search_belowMinLength_returnsEmpty_withoutQueryingRepository() throws Exception {
    mockMvc.perform(get("/api/user/search").param("email", "a"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$").isEmpty());

    verify(userRepository, never()).findByLikeEmail(anyString(), any(Pageable.class));
  }

  @Test
  void search_capsResultsAtThreeViaPageable() throws Exception {
    when(userRepository.findByLikeEmail(anyString(), any(Pageable.class))).thenReturn(List.of());

    mockMvc.perform(get("/api/user/search").param("email", "someone"))
        .andExpect(status().isOk());

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(userRepository).findByLikeEmail(anyString(), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
  }

  @Test
  void getUserById_notFound_returns404() throws Exception {
    when(userRepository.findUserCollectionById("missing")).thenReturn(null);

    mockMvc.perform(get("/api/user/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  void update_userDoesNotExist_returns404() throws Exception {
    when(userRepository.findUserCollectionById(ME)).thenReturn(null);

    mockMvc.perform(patch("/api/user/update/" + ME)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"about\":\"my new bio\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    verify(userRepository, never()).save(any(UserCollection.class));
  }

  @Test
  void getUserFollowing_issuesSingleBatchQuery_notOnePerUser() throws Exception {
    UserCollection u2 = new UserCollection();
    u2.setId(SOMEONE_ELSE);
    when(userRepository.findAllById(any())).thenReturn(List.of(u2));

    mockMvc.perform(post("/api/user/getUserFollow")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"list\":[\"user-2\",\"user-3\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));

    verify(userRepository).findAllById(List.of("user-2", "user-3"));
    verify(userRepository, never()).findUserCollectionById(anyString());
  }

  @Test
  void interactiveUser_self_isBadRequest() throws Exception {
    mockMvc.perform(patch("/api/user/interactive/" + ME))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

    verify(userRepository, never()).findUserCollectionById(anyString());
  }

  @Test
  void interactiveUser_currentUserMissing_returns404() throws Exception {
    when(userRepository.findUserCollectionById(ME)).thenReturn(null);

    mockMvc.perform(patch("/api/user/interactive/" + SOMEONE_ELSE))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(UserCollection.class));
  }

  @Test
  void interactiveUser_follow_usesAtomicAddToSet_onBothSides() throws Exception {
    UserCollection me = new UserCollection();
    me.setId(ME);
    me.setFollowing(List.of());
    UserCollection target = new UserCollection();
    target.setId(SOMEONE_ELSE);
    when(userRepository.findUserCollectionById(ME)).thenReturn(me);
    when(userRepository.findUserCollectionById(SOMEONE_ELSE)).thenReturn(target);

    mockMvc.perform(patch("/api/user/interactive/" + SOMEONE_ELSE))
        .andExpect(status().isOk());

    ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
    verify(mongoTemplate, org.mockito.Mockito.times(2))
        .updateFirst(any(Query.class), updateCaptor.capture(), eq(UserCollection.class));
    List<Update> updates = updateCaptor.getAllValues();
    assertThat(updates.get(0).getUpdateObject().toString()).contains("$addToSet");
    assertThat(updates.get(1).getUpdateObject().toString()).contains("$addToSet");
    verify(userRepository, never()).save(any(UserCollection.class));
  }

  @Test
  void interactiveUser_unfollow_usesAtomicPull_onBothSides() throws Exception {
    UserCollection me = new UserCollection();
    me.setId(ME);
    me.setFollowing(List.of(SOMEONE_ELSE));
    UserCollection target = new UserCollection();
    target.setId(SOMEONE_ELSE);
    when(userRepository.findUserCollectionById(ME)).thenReturn(me);
    when(userRepository.findUserCollectionById(SOMEONE_ELSE)).thenReturn(target);

    mockMvc.perform(patch("/api/user/interactive/" + SOMEONE_ELSE))
        .andExpect(status().isOk());

    ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
    verify(mongoTemplate, org.mockito.Mockito.times(2))
        .updateFirst(any(Query.class), updateCaptor.capture(), eq(UserCollection.class));
    List<Update> updates = updateCaptor.getAllValues();
    assertThat(updates.get(0).getUpdateObject().toString()).contains("$pull");
    assertThat(updates.get(1).getUpdateObject().toString()).contains("$pull");
  }
}
