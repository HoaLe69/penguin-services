package com.example.social_be.controller;

import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.UserCollection;
import com.example.social_be.model.custom.CustomUserDetail;
import com.example.social_be.model.request.RequestList;
import com.example.social_be.model.request.UserUpdateRequest;
import com.example.social_be.model.response.MessageResponse;
import com.example.social_be.model.response.UserResponse;
import com.example.social_be.repository.CommentRepository;
import com.example.social_be.repository.PostRepository;
import com.example.social_be.repository.UserRepository;
import com.example.social_be.security.SecurityUtils;
import com.example.social_be.util.Utilties;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping(value = "/api/user")
public class UserController {

  private static final int SEARCH_MIN_LENGTH = 2;
  private static final int SEARCH_MAX_RESULTS = 3;

  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PasswordEncoder encoder;

  @Autowired
  private PostRepository postRepository;

  @Autowired
  private CommentRepository commentRepository;

  @GetMapping("/search")
  public ResponseEntity<?> searchUser(@RequestParam String email) {
    String query = email == null ? "" : email.trim();
    if (query.length() < SEARCH_MIN_LENGTH) {
      return ResponseEntity.ok(List.of());
    }
    String pattern = Utilties.anchoredLiteralPrefix(query);
    List<UserResponse> results = userRepository
        .findByLikeEmail(pattern, PageRequest.of(0, SEARCH_MAX_RESULTS))
        .stream()
        .map(UserResponse::new)
        .collect(Collectors.toList());
    return ResponseEntity.ok(results);
  }

  @GetMapping("/verify")
  public ResponseEntity<?> verifyUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null) {
      CustomUserDetail userDetail = (CustomUserDetail) authentication.getPrincipal();

      return ResponseEntity.ok(new UserResponse(userDetail.get_id(), userDetail.getUsername(), userDetail.getEmail(),
          userDetail.getDisplayName(), userDetail.getAvatar(), userDetail.getAvatar(), userDetail.getFollower(),
          userDetail.getFollowing()));
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User is not authenticated");
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getUserById(@PathVariable String id) {
    UserCollection userCollection = userRepository.findUserCollectionById(id);
    if (userCollection == null)
      throw ResourceNotFoundException.of("User", id);
    return ResponseEntity.ok(new UserResponse(userCollection));
  }

  @PostMapping("/getUserFollow")
  public ResponseEntity<?> getUserFollowing(@Valid @RequestBody RequestList following) {
    List<UserResponse> userFollowing = StreamSupport
        .stream(userRepository.findAllById(following.getList()).spliterator(), false)
        .map(UserResponse::new)
        .collect(Collectors.toList());
    return ResponseEntity.ok(userFollowing);
  }

  // update user by id
  @PatchMapping("/update/{id}")
  public ResponseEntity<?> updateUser(@Valid @RequestBody UserUpdateRequest update, @PathVariable String id) {
    SecurityUtils.requireSelf(id);
    UserCollection user = userRepository.findUserCollectionById(id);
    if (user == null)
      throw ResourceNotFoundException.of("User", id);
    user.setAbout(update.getAbout());
    UserCollection savedUser = userRepository.save(user);
    return ResponseEntity.ok(new UserResponse(savedUser));
  }

  // delete user by id
  @DeleteMapping("/delete/{id}")
  public ResponseEntity<?> deleteUser(@PathVariable String id) {
    SecurityUtils.requireSelf(id);
    userRepository.deleteById(id);
    return ResponseEntity.ok(new MessageResponse("Delete Successfully !!"));
  }

  // follow and unfollow
  // NOTE: this updates two user documents (follower/following on each side);
  // it's a good candidate to wrap in a real transaction once Mongo is
  // configured as a replica set (REF-20) - @Transactional was removed here
  // because on a standalone Mongo instance it silently does nothing.
  @PatchMapping("/interactive/{visiter}")
  public ResponseEntity<?> interactiveUser(@PathVariable String visiter) {
    String currentId = SecurityUtils.currentUserId();
    if (!currentId.equals(visiter)) {
      UserCollection currentUser = userRepository.findUserCollectionById(currentId);
      UserCollection userFollow = userRepository.findUserCollectionById(visiter);

      // list following of user login
      List<String> listFollowing = currentUser.getFollowing();
      // list follower of visiter
      List<String> listFollower = userFollow.getFollower();
      if (!listFollowing.contains(visiter)) {
        // follow
        listFollowing.add(visiter);
        currentUser.setFollowing(listFollowing);
        userRepository.save(currentUser);
        listFollower.add(currentId);
        userFollow.setFollower(listFollower);
        userRepository.save(userFollow);
        return ResponseEntity.ok(new UserResponse(userFollow));
      } else {
        // unfolllow
        listFollowing.remove(visiter);
        currentUser.setFollowing(listFollowing);
        userRepository.save(currentUser);
        listFollower.remove(currentId);
        userFollow.setFollower(listFollower);
        userRepository.save(userFollow);
        return ResponseEntity.ok(new UserResponse(userFollow));
      }
    } else {
      return ResponseEntity.badRequest().body(new MessageResponse("You can't not follow yourself!!!"));
    }
  }
}
