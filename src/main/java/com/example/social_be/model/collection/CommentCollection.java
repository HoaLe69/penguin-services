package com.example.social_be.model.collection;

import com.example.social_be.util.Utilties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

// Backs CommentRepository.findAllByPostId(id, Pageable), sorted by
// createAt desc (see CommentController.getAllComment).
@CompoundIndex(name = "postId_createAt_idx", def = "{'postId': 1, 'createAt': -1}")
@Document("comment")
@Data
@AllArgsConstructor
public class CommentCollection {
  private String id;
  private String postId;
  private String avatar;
  private String displayName;
  private String userId;
  private String replyTo;
  private String content;
  private String level;
  private ArrayList<String> subCommentIds;
  private Date createAt;

  public CommentCollection(String userId, String avatar, String postId, String content, String displayName) {
    this.userId = userId;
    this.avatar = avatar;
    this.postId = postId;
    this.content = content;
    this.displayName = displayName;
    this.subCommentIds = new ArrayList<>();
    this.createAt = new Date();
  }

  public CommentCollection(String userId, String avatar, String content, String displayName, String replyId,
      String subCommentId) {
    this.id = subCommentId;
    this.userId = userId;
    this.avatar = avatar;
    this.content = content;
    this.displayName = displayName;
    this.replyTo = replyId;
    this.createAt = new Date();
  }

  public CommentCollection() {
    this.createAt = new Date();
    this.subCommentIds = new ArrayList<>();
  }
}
