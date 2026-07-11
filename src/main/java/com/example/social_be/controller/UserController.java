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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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

  @Autowired
  private MongoTemplate mongoTemplate;

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
  // This updates two user documents (follower/following on each side).
  // REF-20 decision: Mongo here is a standalone instance, not a replica set,
  // so multi-document @Transactional would be a silent no-op (worse than not
  // having it, since it looks safe but isn't) - not worth standing up a
  // replica set just for this one call site before the service layer
  // (REF-13) exists to scope transactions properly. Instead, each side is
  // updated with an atomic single-document $addToSet/$pull instead of a
  // read-modify-save of the whole document, which removes the lost-update
  // race (two concurrent requests overwriting each other's array changes)
  // even though the two documents still aren't updated as a single unit.
  @PatchMapping("/interactive/{visiter}")
  public ResponseEntity<?> interactiveUser(@PathVariable String visiter) {
    String currentId = SecurityUtils.currentUserId();
    if (currentId.equals(visiter)) {
      return ResponseEntity.badRequest().body(new MessageResponse("You can't not follow yourself!!!"));
    }

    UserCollection currentUser = userRepository.findUserCollectionById(currentId);
    UserCollection userFollow = userRepository.findUserCollectionById(visiter);
    if (currentUser == null)
      throw ResourceNotFoundException.of("User", currentId);
    if (userFollow == null)
      throw ResourceNotFoundException.of("User", visiter);

    boolean alreadyFollowing = currentUser.getFollowing() != null && currentUser.getFollowing().contains(visiter);
    Update currentUserUpdate = alreadyFollowing
        ? new Update().pull("following", visiter)
        : new Update().addToSet("following", visiter);
    Update targetUpdate = alreadyFollowing
        ? new Update().pull("follower", currentId)
        : new Update().addToSet("follower", currentId);

    mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(currentId)), currentUserUpdate, UserCollection.class);
    mongoTemplate.updateFirst(Query.query(Criteria.where("id").is(visiter)), targetUpdate, UserCollection.class);

    UserCollection updatedTarget = userRepository.findUserCollectionById(visiter);
    return ResponseEntity.ok(new UserResponse(updatedTarget));
  }
}
