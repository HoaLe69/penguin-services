package com.example.social_be;

import com.example.social_be.config.SocialAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@EnableConfigurationProperties(SocialAppProperties.class)
public class SocialBeApplication {

  public static void main(String[] args) {
    SpringApplication.run(SocialBeApplication.class, args);
  }

}
