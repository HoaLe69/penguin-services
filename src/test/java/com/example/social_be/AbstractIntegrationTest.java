package com.example.social_be;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for tests that need the full Spring context against a real
 * (ephemeral, Testcontainers-managed) Mongo instance, instead of an
 * external one a developer/CI would otherwise have to provide by hand.
 * {@code @ServiceConnection} wires spring.data.mongodb.* from the running
 * container automatically - no manual property configuration needed.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

  @Container
  @ServiceConnection
  static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(DockerImageName.parse("mongo:7"));
}
