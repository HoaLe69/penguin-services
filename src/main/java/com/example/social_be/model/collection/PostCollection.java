package com.example.social_be.model.collection;

import com.example.social_be.util.Utilties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Backs the followed-users feed (PostRepository.findByUserIdIn, sorted by
// createAt desc) and the all-posts feed (findAll(Pageable) sorted the same
// way) with a single index instead of a collection scan.
@CompoundIndex(name = "userId_createAt_idx", def = "{'userId': 1, 'createAt': -1}")
@Document(value = "posts")
@Data
@AllArgsConstructor
@Builder
@Jacksonized
public class PostCollection {
  private String id;
  private String userId;
  private String cloudinaryId;
  private String photoUrl;
  private String displayName;
  private String tag;
  private String thumbnail;
  private List<String> like;
  private String description;
  private long comments;
  private String videoSrc;
  private String fileType;
  private Date createAt;

  public PostCollection(String userId, String photoUrl, String displayName, String tag, String thumbnail,
      String cloudinaryId, String description, String fileType, String videoSrc) {
    this.userId = userId;
    this.photoUrl = photoUrl;
    this.displayName = displayName;
    this.tag = tag;
    this.thumbnail = thumbnail;
    this.cloudinaryId = cloudinaryId;
    this.like = new ArrayList<>();
    this.description = description;
    this.comments = 0;
    this.createAt = new Date();
    this.fileType = fileType;
    this.videoSrc = videoSrc;

  }

  public PostCollection() {
    this.createAt = new Date();
    this.videoSrc = null;
    this.fileType = null;
  }
}
