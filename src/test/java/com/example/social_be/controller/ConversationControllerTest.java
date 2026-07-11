package com.example.social_be.controller;

import com.example.social_be.exception.GlobalExceptionHandler;
import com.example.social_be.model.collection.ConversationCollection;
import com.example.social_be.repository.ConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

  @Mock
  private ConversationRepository conversationRepository;
  @Mock
  private MongoTemplate mongoTemplate;
  @InjectMocks
  private ConversationController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  void getAllRoomConversation_usesSingleQuery_notFindAllPlusFilter() throws Exception {
    ConversationCollection room = new ConversationCollection();
    room.setId("room-1");
    room.setMember(List.of("user-1", "user-2"));
    when(conversationRepository.findByMemberContaining("user-1")).thenReturn(List.of(room));

    mockMvc.perform(get("/api/conversation/all/user-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value("room-1"));

    verify(conversationRepository, never()).findAll();
  }

  @Test
  void updateLastestMessage_conversationNotFound_returns404() throws Exception {
    when(conversationRepository.findConversationCollectionById("missing")).thenReturn(null);

    mockMvc.perform(patch("/api/conversation/update/lastestMessage/missing")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"lastestMessage\":\"hi\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));

    verify(conversationRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }
}
