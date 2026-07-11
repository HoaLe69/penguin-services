package com.example.social_be.controller;

import com.example.social_be.exception.GlobalExceptionHandler;
import com.example.social_be.model.collection.MessageCollection;
import com.example.social_be.repository.MessageRepository;
import com.example.social_be.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

  @Mock
  private MessageRepository messageRepository;
  @InjectMocks
  private MessageService messageService;

  private MessageController controller;
  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    controller = new MessageController();
    ReflectionTestUtils.setField(controller, "messageService", messageService);

    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void getAllMess_delegatesToRepository() throws Exception {
    MessageCollection message = new MessageCollection();
    message.setId("m1");
    when(messageRepository.findAllByConversationId("conv-1")).thenReturn(List.of(message));

    mockMvc.perform(get("/api/message/all/conv-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("m1"));
  }

  @Test
  void recallMessage_notFound_returnsBadRequest() throws Exception {
    when(messageRepository.findMessageCollectionById("missing")).thenReturn(null);

    mockMvc.perform(patch("/api/message/recall/missing"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

    verify(messageRepository, never()).save(any());
  }

  @Test
  void recallMessage_found_clearsContent() throws Exception {
    MessageCollection message = new MessageCollection();
    message.setId("m1");
    message.setContent("secret");
    when(messageRepository.findMessageCollectionById("m1")).thenReturn(message);
    when(messageRepository.save(any(MessageCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    mockMvc.perform(patch("/api/message/recall/m1"))
        .andExpect(status().isOk());

    verify(messageRepository).save(message);
  }
}
