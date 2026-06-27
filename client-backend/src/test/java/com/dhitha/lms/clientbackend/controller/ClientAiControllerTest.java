package com.dhitha.lms.clientbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dhitha.lms.clientbackend.dto.AiChatRequestDTO;
import com.dhitha.lms.clientbackend.dto.AiChatResponseDTO;
import com.dhitha.lms.clientbackend.dto.UserDTO;
import com.dhitha.lms.clientbackend.service.LibraryAiAssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ClientAiControllerTest {

  @Test
  void chatReturnsAssistantResponse() {
    LibraryAiAssistantService service = mock(LibraryAiAssistantService.class);
    AiChatRequestDTO request = new AiChatRequestDTO("s1", "推荐 Java 图书");
    AiChatResponseDTO response =
        AiChatResponseDTO.builder()
            .sessionId("s1")
            .intent("BOOK_RECOMMENDATION")
            .answer("推荐 Java Core Technology")
            .modelProvider("local-rule-rag-demo")
            .build();
    when(service.chat(request, 2L)).thenReturn(response);

    ResponseEntity<AiChatResponseDTO> entity =
        new ClientAiController(service).chat(request, authentication());

    assertThat(entity.getStatusCodeValue()).isEqualTo(200);
    assertThat(entity.getBody().getIntent()).isEqualTo("BOOK_RECOMMENDATION");
    verify(service).chat(request, 2L);
  }

  @Test
  void streamChatReturnsEmitterAndStartsAssistantStream() {
    LibraryAiAssistantService service = mock(LibraryAiAssistantService.class);
    AiChatRequestDTO request = new AiChatRequestDTO("s1", "stream Java books");

    SseEmitter emitter = new ClientAiController(service).streamChat(request, authentication());

    assertThat(emitter).isNotNull();
    verify(service).streamChat(request, 2L, emitter);
  }

  private Authentication authentication() {
    UserDTO user = new UserDTO();
    user.setId(2L);
    return new UsernamePasswordAuthenticationToken(user, null);
  }
}
