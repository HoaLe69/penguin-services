package com.example.social_be.controller;

import com.example.social_be.exception.GlobalExceptionHandler;
import com.example.social_be.model.collection.UserCollection;
import com.example.social_be.repository.UserRepository;
import com.example.social_be.util.CookieService;
import com.example.social_be.util.JwtTokenUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

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
  @InjectMocks
  private AuthController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void login_unknownUsername_returns400WithUnifiedErrorEnvelope() throws Exception {
    when(userRepository.findUserCollectionByUserName("nobody")).thenReturn(null);

    mockMvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"userName\":\"nobody\",\"password\":\"password123\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("Username does not exist"));

    verify(cookieService, never()).attachAuthCookies(any(), anyString(), anyLong(), anyString(), anyLong());
  }

  @Test
  void register_existingUsername_returns400WithUnifiedErrorEnvelope() throws Exception {
    when(userRepository.findUserCollectionByUserName("taken")).thenReturn(new UserCollection());

    mockMvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"userName\":\"taken\",\"email\":\"a@b.com\",\"password\":\"password123\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("Username already exists"));

    verify(userRepository, never()).save(any());
  }

  @Test
  void refreshToken_blankCookie_returns400WithUnifiedErrorEnvelope() throws Exception {
    mockMvc.perform(post("/api/auth/refresh-token")
        .cookie(new Cookie("refreshToken", "")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
        .andExpect(jsonPath("$.message").value("You are not authenticated"));
  }
}
