package com.example.social_be.model.collection;

import com.example.social_be.model.request.AuthSignUpRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
@Data
@Builder
@AllArgsConstructor
public class UserCollection {
  private String id;
  // sparse: most users don't have a socialId (only Google-login accounts
  // do), and a plain unique index would treat every null as a duplicate.
  @JsonIgnore
  @Indexed(unique = true, sparse = true)
  private String socialId;
  @Indexed(unique = true)
  private String userName;
  @JsonIgnore
  private String password;
  @Indexed
  private String email;
  private String displayName;
  private String avatar;
  private String about;
  private List<String> follower;
  private List<String> following;

  public UserCollection() {
    this.displayName = null;
    this.avatar = null;
    this.about = null;
    this.follower = new ArrayList<>();
    this.following = new ArrayList<>();
  }

  public UserCollection(AuthSignUpRequest authSignUpRequest) {
    this.userName = authSignUpRequest.getUserName();
    this.email = authSignUpRequest.getEmail();
    this.password = authSignUpRequest.getPassword();
    this.displayName = authSignUpRequest.getUserName();
    this.avatar = null;
    this.about = null;
    this.follower = new ArrayList<>();
    this.following = new ArrayList<>();
  }
}
