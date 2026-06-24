package com.dhitha.lms.clientbackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dhitha.lms.clientbackend.dto.AiChatRequestDTO;
import com.dhitha.lms.clientbackend.dto.AiChatResponseDTO;
import com.dhitha.lms.clientbackend.service.LibraryAiAssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

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
    when(service.chat(request)).thenReturn(response);

    ResponseEntity<AiChatResponseDTO> entity = new ClientAiController(service).chat(request);

    assertThat(entity.getStatusCodeValue()).isEqualTo(200);
    assertThat(entity.getBody().getIntent()).isEqualTo("BOOK_RECOMMENDATION");
  }
}
