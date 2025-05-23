package com.example.social_be.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class SendEmailService {
  @Autowired
  private JavaMailSender mailSender;
  @Autowired
  private TemplateEngine templateEngine;

  public void sendEmail(String to, String subject, String body) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);
    message.setSubject(subject);
    message.setText(body);
    mailSender.send(message);
  }

  public void sendEmailWithHtmlTemplate(String to, String subject, String templateName, Context context) {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
    try {
      helper.setTo(to);
      helper.setSubject(subject);
      String htmlContent = templateEngine.process(templateName, context);
      helper.setText(htmlContent, true);
      mailSender.send(mimeMessage);
    } catch (Exception ex) {

    }
  }
}
