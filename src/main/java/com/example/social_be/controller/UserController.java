package com.example.social_be.controller;

import com.example.social_be.model.custom.CustomUserDetail;
import com.example.social_be.model.request.RequestList;
import com.example.social_be.model.request.UserUpdateRequest;
import com.example.social_be.model.response.MessageResponse;
import com.example.social_be.model.response.UserResponse;
import com.example.social_be.security.SecurityUtils;
import com.example.social_be.service.UserService;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/user")
public class UserController {

  @Autowired
  private UserService userService;

  @GetMapping("/search")
  public ResponseEntity<?> searchUser(@RequestParam String email) {
    return ResponseEntity.ok(userService.searchUsers(email));
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
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @PostMapping("/getUserFollow")
  public ResponseEntity<?> getUserFollowing(@Valid @RequestBody RequestList following) {
    return ResponseEntity.ok(userService.getUserFollowing(following.getList()));
  }

  // update user by id
  @PatchMapping("/update/{id}")
  public ResponseEntity<?> updateUser(@Valid @RequestBody UserUpdateRequest update, @PathVariable String id) {
    return ResponseEntity.ok(userService.updateUser(id, update));
  }

  // delete user by id
  @DeleteMapping("/delete/{id}")
  public ResponseEntity<?> deleteUser(@PathVariable String id) {
    userService.deleteUser(id);
    return ResponseEntity.ok(new MessageResponse("Delete Successfully !!"));
  }

  // follow and unfollow
  @PatchMapping("/interactive/{visiter}")
  public ResponseEntity<?> interactiveUser(@PathVariable String visiter) {
    String currentId = SecurityUtils.currentUserId();
    if (currentId.equals(visiter)) {
      return ResponseEntity.badRequest().body(new MessageResponse("You can't not follow yourself!!!"));
    }
    return ResponseEntity.ok(userService.interactiveUser(currentId, visiter));
  }
}
