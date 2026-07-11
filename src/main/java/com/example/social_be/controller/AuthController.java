package com.example.social_be.controller;

import com.example.social_be.model.collection.UserCollection;
import com.example.social_be.model.request.AuthLoginGoogleRequest;
import com.example.social_be.model.request.AuthLoginRequest;
import com.example.social_be.model.request.AuthSignUpRequest;
import com.example.social_be.model.response.JwtResponse;
import com.example.social_be.model.response.MessageResponse;
import com.example.social_be.repository.UserRepository;
import com.example.social_be.util.CookieService;
import com.example.social_be.util.JwtTokenUtil;
import com.example.social_be.util.Utilties;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  @Autowired
  private JwtTokenUtil jwtTokenUtil;
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private PasswordEncoder encoder;
  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private CookieService cookieService;

  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody AuthLoginRequest authLoginRequest, HttpServletResponse response) {
    UserCollection userCheck = userRepository.findUserCollectionByUserName(authLoginRequest.getUserName());
    if (userCheck == null)
      throw new IllegalArgumentException("Username does not exist");
    // A wrong password throws BadCredentialsException here, which the
    // GlobalExceptionHandler maps to 401 (previously an unhandled 500).
    Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
        authLoginRequest.getUserName(), authLoginRequest.getPassword()));
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String accessToken = jwtTokenUtil.generateJwtAccessToken(authLoginRequest.getUserName());
    String refreshToken = jwtTokenUtil.generateJwtRefreshToken(authLoginRequest.getUserName());

    cookieService.attachAuthCookies(response,
        accessToken, jwtTokenUtil.getAccessTokenValiditySeconds(),
        refreshToken, jwtTokenUtil.getRefreshTokenValiditySeconds());

    return ResponseEntity.ok(new MessageResponse("Login successfully"));
  }

  @PostMapping("/loginWithSocial")
  public ResponseEntity<?> loginWithSocial(@Valid @RequestBody AuthLoginGoogleRequest userInfo, HttpServletResponse response) {
    UserCollection storedUser = userRepository.findUserCollectionBySocialId(userInfo.getId());
    String username = Utilties.extractUsername(userInfo.getEmail());

    if (storedUser == null) {
      UserCollection userCollection = new UserCollection();
      userCollection.setSocialId(userInfo.getId());
      userCollection.setAvatar(userInfo.getPicture());
      userCollection.setUserName(username);
      userCollection.setDisplayName(userInfo.getGiven_name());
      userCollection.setEmail(userInfo.getEmail());
      userRepository.save(userCollection);
    }
    String accessToken = jwtTokenUtil.generateJwtAccessToken(username);
    String refreshToken = jwtTokenUtil.generateJwtRefreshToken(username);

    cookieService.attachAuthCookies(response,
        accessToken, jwtTokenUtil.getAccessTokenValiditySeconds(),
        refreshToken, jwtTokenUtil.getRefreshTokenValiditySeconds());

    return ResponseEntity.ok(new MessageResponse("ok"));
  }

  @GetMapping("/log-out/{name}")
  public ResponseEntity<?> logOut(@PathVariable String name, HttpServletResponse response) {
    cookieService.clearAuthCookies(response);
    return ResponseEntity.ok(new MessageResponse("ok"));
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@Valid @RequestBody AuthSignUpRequest authSignUpRequest) {
    UserCollection user = userRepository.findUserCollectionByUserName(authSignUpRequest.getUserName());
    if (user != null)
      throw new IllegalArgumentException("Username already exists");
    String pass = encoder.encode(authSignUpRequest.getPassword());
    authSignUpRequest.setPassword(pass);
    UserCollection userCollection = new UserCollection(authSignUpRequest);
    userRepository.save(userCollection);
    return ResponseEntity.ok(new MessageResponse("Register successfully"));
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<?> refreshToken(@CookieValue("refreshToken") String token, HttpServletResponse response) {
    if (StringUtils.hasText(token)) {
      String userName = jwtTokenUtil.getUserNameFromRefreshToken(token);
      UserCollection userCollection = userRepository.findUserCollectionByUserName(userName);
      if (userCollection != null && jwtTokenUtil.validateJwtRefreshToken(token, userName)) {
        String accessToken = jwtTokenUtil.generateJwtAccessToken(userName);
        String refreshToken = jwtTokenUtil.generateJwtRefreshToken(userName);
        cookieService.attachAuthCookies(response,
            accessToken, jwtTokenUtil.getAccessTokenValiditySeconds(),
            refreshToken, jwtTokenUtil.getRefreshTokenValiditySeconds());
        return ResponseEntity.ok(new JwtResponse(accessToken, refreshToken));
      }
    }
    throw new IllegalArgumentException("You are not authenticated");
  }

}
