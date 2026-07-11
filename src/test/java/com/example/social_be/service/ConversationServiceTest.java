package com.example.social_be.service;

import com.example.social_be.exception.ResourceNotFoundException;
import com.example.social_be.model.collection.ConversationCollection;
import com.example.social_be.model.response.ConversationResponse;
import com.example.social_be.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

  @Mock
  private ConversationRepository conversationRepository;
  @Mock
  private MongoTemplate mongoTemplate;
  @InjectMocks
  private ConversationService conversationService;

  @Test
  void getAllRoomConversation_usesSingleQuery_notFindAllPlusFilter() {
    ConversationCollection room = new ConversationCollection();
    room.setId("room-1");
    room.setMember(List.of("user-1", "user-2"));
    when(conversationRepository.findByMemberContaining("user-1")).thenReturn(List.of(room));

    List<ConversationResponse> result = conversationService.getAllRoomConversation("user-1");

    assertThat(result).containsExactly(new ConversationResponse(room));
    verify(conversationRepository, never()).findAll();
  }

  @Test
  void findConversation_notFound_returnsNull() {
    when(mongoTemplate.findOne(any(Query.class), org.mockito.ArgumentMatchers.eq(ConversationCollection.class),
        org.mockito.ArgumentMatchers.eq("room"))).thenReturn(null);

    assertThat(conversationService.findConversation("user-1", "user-2")).isNull();
  }

  @Test
  void updateLastestMessage_conversationNotFound_throwsResourceNotFound() {
    when(conversationRepository.findConversationCollectionById("missing")).thenReturn(null);

    assertThatThrownBy(() -> conversationService.updateLastestMessage("missing", "hi"))
        .isInstanceOf(ResourceNotFoundException.class);

    verify(conversationRepository, never()).save(any());
  }

  @Test
  void updateLastestMessage_found_updatesAndSaves() {
    ConversationCollection conversation = new ConversationCollection();
    conversation.setId("room-1");
    when(conversationRepository.findConversationCollectionById("room-1")).thenReturn(conversation);
    when(conversationRepository.save(any(ConversationCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    ConversationResponse result = conversationService.updateLastestMessage("room-1", "hi there");

    assertThat(result.getLastestMessage()).isEqualTo("hi there");
  }

  @Test
  void createConversation_savesWithMembers() {
    when(conversationRepository.save(any(ConversationCollection.class))).thenAnswer(inv -> inv.getArgument(0));

    ConversationResponse result = conversationService.createConversation(List.of("user-1", "user-2"));

    assertThat(result.getMember()).containsExactly("user-1", "user-2");
  }
}
