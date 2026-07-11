package com.example.social_be.model.response;

import com.example.social_be.model.collection.CommentCollection;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CommentResponse {
  private String id;
  private String postId;
  private String avatar;
  private String displayName;
  private String userId;
  private String replyTo;
  private String content;
  private String level;
  private List<String> subCommentIds;
  private Date createAt;

  public CommentResponse(CommentCollection comment) {
    this.id = comment.getId();
    this.postId = comment.getPostId();
    this.avatar = comment.getAvatar();
    this.displayName = comment.getDisplayName();
    this.userId = comment.getUserId();
    this.replyTo = comment.getReplyTo();
    this.content = comment.getContent();
    this.level = comment.getLevel();
    this.subCommentIds = comment.getSubCommentIds();
    this.createAt = comment.getCreateAt();
  }
}
