package com.example.social_be.controller;

import com.example.social_be.model.collection.UserCollection;
import com.example.social_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

import java.util.List;

@Controller
public class GreetController {
  @Autowired
  private UserRepository userRepository;
  @GetMapping("/hi")
  public String greet(Model model) {
    List<UserCollection> users = userRepository.findAll();

    model.addAttribute("message", "Welcome to Penguin!!!");
    model.addAttribute("users" , users);
    return "home"; // refres to resource/templates/home.html
  }
}
