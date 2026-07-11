package com.example.social_be.service;

import com.example.social_be.model.collection.MessageCollection;
import com.example.social_be.model.request.MessageRequestSocket;
import com.example.social_be.model.response.MessageResponse;
import com.example.social_be.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @Mock
  private MessageRepository messageRepository;
  @InjectMocks
  private MessageService messageService;

  @Test
  void create_savesMessage() {
    MessageRequestSocket request = new MessageRequestSocket("user-1", "hello", null, null, 0, "conv-1", null);
    when(messageRepository.save(any(MessageCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    Object result = messageService.handleSocketMessage("conv-1", request);

    assertThat(result).isInstanceOf(MessageCollection.class);
    MessageCollection saved = (MessageCollection) result;
    assertThat(saved.getContent()).isEqualTo("hello");
    assertThat(saved.getUserId()).isEqualTo("user-1");
    assertThat(saved.getConversationId()).isEqualTo("conv-1");
  }

  @Test
  void delete_doesNotTouchRepository_returnsIdEcho() {
    MessageRequestSocket request = new MessageRequestSocket("user-1", null, null, "msg-1", 1, "conv-1", null);

    Object result = messageService.handleSocketMessage("conv-1", request);

    assertThat(result).isInstanceOf(MessageResponse.class);
    verify(messageRepository, never()).save(any());
  }
}
