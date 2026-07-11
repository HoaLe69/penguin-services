package com.example.social_be.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

  @Autowired
  private SocialAppProperties properties;

  @Bean
  public WebMvcConfigurer corsConfigurer() {

    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(properties.getCors().getAllowedOrigins().toArray(new String[0]))
            .allowCredentials(true)
            .allowedMethods("*")
            .exposedHeaders("Set-Cookie");
      }
    };
  }
}
