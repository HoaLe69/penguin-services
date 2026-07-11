package com.example.social_be.repository;

import com.example.social_be.model.collection.PostCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends MongoRepository<PostCollection, String> {
  PostCollection findPostCollectionById(String id);

  List<PostCollection> findAllByUserId(String id);

  // Single $in query for a feed of followed users, instead of one query per
  // user id.
  Page<PostCollection> findByUserIdIn(Collection<String> userIds, Pageable pageable);
}
