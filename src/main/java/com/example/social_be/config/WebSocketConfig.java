package com.example.social_be.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  @Autowired
  private SocialAppProperties properties;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    // Registers the endpoint where the connection will take place
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns(properties.getCors().getAllowedOrigins().toArray(new String[0]))
        // Enable SockJS fallback options
        .withSockJS();
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
    // Set prefix for the endpoint that the client listens for our messages from
    config.enableSimpleBroker("/topic/", "/queue/");
    // Set prefix for endpoints the client will send messages to
    config.setApplicationDestinationPrefixes("/app");
  }

}
