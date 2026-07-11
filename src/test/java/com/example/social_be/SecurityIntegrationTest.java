package com.example.social_be;

import com.example.social_be.model.collection.UserCollection;
import com.example.social_be.repository.UserRepository;
import com.example.social_be.util.JwtTokenUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real filter chain (JwtRequestFilter + SecurityConfiguration
 * + JwtAuthenticationEntryPoint), not a standalone controller instance -
 * this is the one place a broken requestMatchers() rule or a filter
 * regression would actually get caught, which none of the
 * MockMvcBuilders.standaloneSetup(...) controller tests elsewhere in this
 * suite can do (they never construct a real SecurityFilterChain).
 */
@AutoConfigureMockMvc
class SecurityIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private JwtTokenUtil jwtTokenUtil;

  private UserCollection me;
  private UserCollection someoneElse;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
    me = userRepository.save(newUser("alice-" + System.nanoTime()));
    someoneElse = userRepository.save(newUser("bob-" + System.nanoTime()));
  }

  @AfterEach
  void tearDown() {
    userRepository.deleteAll();
  }

  private UserCollection newUser(String userName) {
    UserCollection user = new UserCollection();
    user.setUserName(userName);
    user.setEmail(userName + "@example.com");
    user.setPassword("unused-in-these-tests");
    user.setDisplayName(userName);
    return user;
  }

  @Test
  void protectedEndpoint_withoutCookie_returns401() throws Exception {
    mockMvc.perform(get("/api/user/verify"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void protectedEndpoint_withGarbageToken_returns401_notServerError() throws Exception {
    mockMvc.perform(get("/api/user/verify").cookie(new Cookie("token", "not-a-real-jwt")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void protectedEndpoint_withValidCookie_returns200() throws Exception {
    String token = jwtTokenUtil.generateJwtAccessToken(me.getUserName());

    mockMvc.perform(get("/api/user/verify").cookie(new Cookie("token", token)))
        .andExpect(status().isOk());
  }

  @Test
  void updateAnotherUsersProfile_withValidCookie_returns403() throws Exception {
    String token = jwtTokenUtil.generateJwtAccessToken(me.getUserName());

    mockMvc.perform(patch("/api/user/update/" + someoneElse.getId())
        .cookie(new Cookie("token", token))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"about\":\"hacked\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateOwnProfile_withValidCookie_returns200() throws Exception {
    String token = jwtTokenUtil.generateJwtAccessToken(me.getUserName());

    mockMvc.perform(patch("/api/user/update/" + me.getId())
        .cookie(new Cookie("token", token))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"about\":\"my real bio\"}"))
        .andExpect(status().isOk());
  }
}
