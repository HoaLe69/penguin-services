package com.example.social_be.model.response;

import com.example.social_be.model.collection.PostCollection;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PostResponse {
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

  public PostResponse(PostCollection post) {
    this.id = post.getId();
    this.userId = post.getUserId();
    this.cloudinaryId = post.getCloudinaryId();
    this.photoUrl = post.getPhotoUrl();
    this.displayName = post.getDisplayName();
    this.tag = post.getTag();
    this.thumbnail = post.getThumbnail();
    this.like = post.getLike();
    this.description = post.getDescription();
    this.comments = post.getComments();
    this.videoSrc = post.getVideoSrc();
    this.fileType = post.getFileType();
    this.createAt = post.getCreateAt();
  }
}
