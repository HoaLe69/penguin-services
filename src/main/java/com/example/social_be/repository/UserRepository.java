package com.example.social_be.repository;

import com.example.social_be.model.collection.UserCollection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRepository extends MongoRepository<UserCollection, String> {
  UserCollection findUserCollectionByUserName(String userName);

  UserCollection findUserCollectionById(String Id);

  UserCollection findUserCollectionBySocialId(String Id);

  /**
   * {@code pattern} must already be a safe, anchored regex (e.g. built via
   * {@code Utilties.anchoredLiteralPrefix}) — callers are responsible for
   * escaping user input before it reaches this query.
   */
  @Query("{ displayName : { $regex : ?0 } }")
  List<UserCollection> findByLikeUserName(String pattern, Pageable pageable);

  /**
   * {@code pattern} must already be a safe, anchored regex (e.g. built via
   * {@code Utilties.anchoredLiteralPrefix}) — callers are responsible for
   * escaping user input before it reaches this query.
   */
  @Query("{ email : { $regex : ?0 } }")
  List<UserCollection> findByLikeEmail(String pattern, Pageable pageable);
}
