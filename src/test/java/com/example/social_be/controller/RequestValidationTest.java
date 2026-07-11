package com.example.social_be.controller;

import com.example.social_be.exception.GlobalExceptionHandler;
import com.example.social_be.repository.UserRepository;
import com.example.social_be.service.ConversationService;
import com.example.social_be.util.CookieService;
import com.example.social_be.util.JwtTokenUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the REF-11 acceptance criterion end to end: invalid payloads return
 * 400 with per-field messages before any repository/service code runs.
 */
@ExtendWith(MockitoExtension.class)
class RequestValidationTest {

  @Mock
  private JwtTokenUtil jwtTokenUtil;
  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder encoder;
  @Mock
  private AuthenticationManager authenticationManager;
  @Mock
  private CookieService cookieService;

  @Mock
  private ConversationService conversationService;

  @Test
  void register_blankUsernameAndInvalidEmailAndShortPassword_returns400WithFieldErrors() throws Exception {
    AuthController controller = new AuthController();
    setField(controller, "jwtTokenUtil", jwtTokenUtil);
    setField(controller, "userRepository", userRepository);
    setField(controller, "encoder", encoder);
    setField(controller, "authenticationManager", authenticationManager);
    setField(controller, "cookieService", cookieService);
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"userName\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors.userName").exists())
        .andExpect(jsonPath("$.fieldErrors.email").exists())
        .andExpect(jsonPath("$.fieldErrors.password").exists());

    verify(userRepository, never()).findUserCollectionByUserName(anyString());
  }

  @Test
  void createConversation_withOneMember_returns400() throws Exception {
    ConversationController controller = new ConversationController();
    setField(controller, "conversationService", conversationService);
    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    mockMvc.perform(post("/api/conversation/create")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"member\":[\"user-1\"]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.fieldErrors.member").exists());

    verify(conversationService, never()).createConversation(org.mockito.ArgumentMatchers.any());
  }

  private static void setField(Object target, String field, Object value) {
    org.springframework.test.util.ReflectionTestUtils.setField(target, field, value);
  }
}
