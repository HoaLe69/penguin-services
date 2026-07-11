package com.example.social_be.service;

import com.example.social_be.exception.ForbiddenException;
import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.UserCollection;
import com.example.social_be.model.custom.CustomUserDetail;
import com.example.social_be.model.request.UserUpdateRequest;
import com.example.social_be.model.response.UserResponse;
import com.example.social_be.repository.UserRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;
  @Mock
  private MongoTemplate mongoTemplate;
  @InjectMocks
  private UserService userService;

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

  @Test
  void searchUsers_belowMinLength_returnsEmpty_withoutQueryingRepository() {
    assertThat(userService.searchUsers("a")).isEmpty();
    verify(userRepository, never()).findByLikeEmail(anyString(), any(Pageable.class));
  }

  @Test
  void searchUsers_escapesRegexSpecialCharacters_andAnchorsAsPrefix() {
    when(userRepository.findByLikeEmail(anyString(), any(Pageable.class))).thenReturn(List.of());

    userService.searchUsers("a.*evil(x)");

    ArgumentCaptor<String> patternCaptor = ArgumentCaptor.forClass(String.class);
    verify(userRepository).findByLikeEmail(patternCaptor.capture(), any(Pageable.class));
    assertThat(patternCaptor.getValue()).startsWith("^").contains("\\Qa.*evil(x)\\E");
  }

  @Test
  void searchUsers_capsResultsAtThreeViaPageable() {
    when(userRepository.findByLikeEmail(anyString(), any(Pageable.class))).thenReturn(List.of());

    userService.searchUsers("someone");

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(userRepository).findByLikeEmail(anyString(), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);
  }

  @Test
  void getUserById_notFound_throwsResourceNotFound() {
    when(userRepository.findUserCollectionById("missing")).thenReturn(null);

    assertThatThrownBy(() -> userService.getUserById("missing"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void getUserFollowing_issuesSingleBatchQuery_notOnePerUser() {
    UserCollection u2 = new UserCollection();
    u2.setId(SOMEONE_ELSE);
    when(userRepository.findAllById(any())).thenReturn(List.of(u2));

    List<UserResponse> result = userService.getUserFollowing(List.of(SOMEONE_ELSE, "user-3"));

    assertThat(result).hasSize(1);
    verify(userRepository).findAllById(List.of(SOMEONE_ELSE, "user-3"));
    verify(userRepository, never()).findUserCollectionById(anyString());
  }

  @Test
  void updateUser_anotherUsersProfile_throwsForbidden() {
    UserUpdateRequest update = new UserUpdateRequest();
    update.setAbout("hacked");

    assertThatThrownBy(() -> userService.updateUser(SOMEONE_ELSE, update))
        .isInstanceOf(ForbiddenException.class);

    verify(userRepository, never()).save(any(UserCollection.class));
  }

  @Test
  void updateUser_ownProfile_succeeds() {
    UserCollection me = new UserCollection();
    me.setId(ME);
    when(userRepository.findUserCollectionById(ME)).thenReturn(me);
    when(userRepository.save(any(UserCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    UserUpdateRequest update = new UserUpdateRequest();
    update.setAbout("my new bio");

    UserResponse result = userService.updateUser(ME, update);

    assertThat(result.getAbout()).isEqualTo("my new bio");
  }

  @Test
  void updateUser_userDoesNotExist_throwsResourceNotFound() {
    when(userRepository.findUserCollectionById(ME)).thenReturn(null);

    UserUpdateRequest update = new UserUpdateRequest();
    update.setAbout("bio");

    assertThatThrownBy(() -> userService.updateUser(ME, update))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(userRepository, never()).save(any(UserCollection.class));
  }

  @Test
  void deleteUser_anotherUser_throwsForbidden() {
    assertThatThrownBy(() -> userService.deleteUser(SOMEONE_ELSE))
        .isInstanceOf(ForbiddenException.class);

    verify(userRepository, never()).deleteById(anyString());
  }

  @Test
  void deleteUser_self_succeeds() {
    userService.deleteUser(ME);

    verify(userRepository).deleteById(ME);
  }

  @Test
  void interactiveUser_currentUserMissing_throwsResourceNotFound() {
    when(userRepository.findUserCollectionById(ME)).thenReturn(null);

    assertThatThrownBy(() -> userService.interactiveUser(ME, SOMEONE_ELSE))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(UserCollection.class));
  }

  @Test
  void interactiveUser_targetMissing_throwsResourceNotFound() {
    UserCollection me = new UserCollection();
    me.setId(ME);
    when(userRepository.findUserCollectionById(ME)).thenReturn(me);
    when(userRepository.findUserCollectionById(SOMEONE_ELSE)).thenReturn(null);

    assertThatThrownBy(() -> userService.interactiveUser(ME, SOMEONE_ELSE))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void interactiveUser_follow_usesAtomicAddToSet_onBothSides() {
    UserCollection me = new UserCollection();
    me.setId(ME);
    me.setFollowing(List.of());
    UserCollection target = new UserCollection();
    target.setId(SOMEONE_ELSE);
    when(userRepository.findUserCollectionById(ME)).thenReturn(me);
    when(userRepository.findUserCollectionById(SOMEONE_ELSE)).thenReturn(target);

    userService.interactiveUser(ME, SOMEONE_ELSE);

    ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
    verify(mongoTemplate, times(2))
        .updateFirst(any(Query.class), updateCaptor.capture(), eq(UserCollection.class));
    List<Update> updates = updateCaptor.getAllValues();
    assertThat(updates.get(0).getUpdateObject().toString()).contains("$addToSet");
    assertThat(updates.get(1).getUpdateObject().toString()).contains("$addToSet");
    verify(userRepository, never()).save(any(UserCollection.class));
  }

  @Test
  void interactiveUser_unfollow_usesAtomicPull_onBothSides() {
    UserCollection me = new UserCollection();
    me.setId(ME);
    me.setFollowing(List.of(SOMEONE_ELSE));
    UserCollection target = new UserCollection();
    target.setId(SOMEONE_ELSE);
    when(userRepository.findUserCollectionById(ME)).thenReturn(me);
    when(userRepository.findUserCollectionById(SOMEONE_ELSE)).thenReturn(target);

    userService.interactiveUser(ME, SOMEONE_ELSE);

    ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
    verify(mongoTemplate, times(2))
        .updateFirst(any(Query.class), updateCaptor.capture(), eq(UserCollection.class));
    List<Update> updates = updateCaptor.getAllValues();
    assertThat(updates.get(0).getUpdateObject().toString()).contains("$pull");
    assertThat(updates.get(1).getUpdateObject().toString()).contains("$pull");
  }
}
