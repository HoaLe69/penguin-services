package com.example.social_be.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the advice maps each exception to the right status and the standard
 * {@link com.example.social_be.dto.ErrorResponse} shape, in isolation from
 * security/Mongo wiring.
 */
class GlobalExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void notFound_maps_to_404() throws Exception {
    mockMvc.perform(get("/test/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.path").value("/test/not-found"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void forbidden_maps_to_403() throws Exception {
    mockMvc.perform(get("/test/forbidden"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void accessDenied_maps_to_403() throws Exception {
    mockMvc.perform(get("/test/access-denied"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void badCredentials_maps_to_401() throws Exception {
    mockMvc.perform(get("/test/bad-credentials"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void illegalArgument_maps_to_400() throws Exception {
    mockMvc.perform(get("/test/bad-request"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
  }

  @Test
  void unexpected_maps_to_500_without_leaking_detail() throws Exception {
    mockMvc.perform(get("/test/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
  }

  @RestController
  static class ThrowingController {
    @GetMapping("/test/not-found")
    void notFound() {
      throw ResourceNotFoundException.of("Post", "123");
    }

    @GetMapping("/test/forbidden")
    void forbidden() {
      throw new ForbiddenException("not the owner");
    }

    @GetMapping("/test/access-denied")
    void accessDenied() {
      throw new AccessDeniedException("denied");
    }

    @GetMapping("/test/bad-credentials")
    void badCredentials() {
      throw new BadCredentialsException("bad");
    }

    @GetMapping("/test/bad-request")
    void badRequest() {
      throw new IllegalArgumentException("invalid input");
    }

    @GetMapping("/test/boom")
    void boom() {
      throw new RuntimeException("internal detail that must not leak");
    }
  }
}
