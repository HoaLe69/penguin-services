package com.example.social_be.service;

import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.UserCollection;
import com.example.social_be.model.custom.CustomUserDetail;
import com.example.social_be.model.request.UserUpdateRequest;
import com.example.social_be.model.response.UserResponse;
import com.example.social_be.repository.UserRepository;
import com.example.social_be.security.SecurityUtils;
import com.example.social_be.util.Utilties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserService implements UserDetailsService {

  private static final int SEARCH_MIN_LENGTH = 2;
  private static final int SEARCH_MAX_RESULTS = 3;

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private MongoTemplate mongoTemplate;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserCollection userCollection = userRepository.findUserCollectionByUserName(username);
    if (userCollection == null)
      throw new UsernameNotFoundException(username);
    return new CustomUserDetail(
        userCollection.getId(),
        userCollection.getUserName(),
        userCollection.getEmail(),
        userCollection.getDisplayName(),
        userCollection.getAvatar(),
        userCollection.getAbout(),
        userCollection.getFollower(),
        userCollection.getFollowing(), new ArrayList<>(),
        userCollection.getPassword());
  }

  public List<UserResponse> searchUsers(String email) {
    String query = email == null ? "" : email.trim();
    if (query.length() < SEARCH_MIN_LENGTH) {
      return List.of();
    }
    String pattern = Utilties.anchoredLiteralPrefix(query);
    return userRepository
        .findByLikeEmail(pattern, PageRequest.of(0, SEARCH_MAX_RESULTS))
        .stream()
        .map(UserResponse::new)
        .collect(Collectors.toList());
  }

  public UserResponse getUserById(String id) {
    UserCollection userCollection = userRepository.findUserCollectionById(id);
    if (userCollection == null)
      throw ResourceNotFoundException.of("User", id);
    return new UserResponse(userCollection);
  }

  public List<UserResponse> getUserFollowing(List<String> ids) {
    return StreamSupport
        .stream(userRepository.findAllById(ids).spliterator(), false)
        .map(UserResponse::new)
        .collect(Collectors.toList());
  }

  public UserResponse updateUser(String id, UserUpdateRequest update) {
    SecurityUtils.requireSelf(id);
    UserCollection user = userRepository.findUserCollectionById(id);
    if (user == null)
      throw ResourceNotFoundException.of("User", id);
    user.setAbout(update.getAbout());
    UserCollection savedUser = userRepository.save(user);
    return new UserResponse(savedUser);
  }

  public void deleteUser(String id) {
    SecurityUtils.requireSelf(id);
    userRepository.deleteById(id);
  }

  // This updates two user documents (follower/following on each side).
  // REF-20 decision: Mongo here is a standalone instance, not a replica set,
  // so multi-document @Transactional would be a silent no-op (worse than not
  // having it, since it looks safe but isn't) - not worth standing up a
  // replica set just for this one call site. Instead, each side is updated
  // with an atomic single-document $addToSet/$pull instead of a
  // read-modify-save of the whole document, which removes the lost-update
  // race (two concurrent requests overwriting each other's array changes)
  // even though the two documents still aren't updated as a single unit.
  public UserResponse interactiveUser(String currentId, String visiter) {
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
    return new UserResponse(updatedTarget);
  }
}
