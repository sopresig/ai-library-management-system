package com.dhitha.lms.clientbackend.controller;

import com.dhitha.lms.clientbackend.dto.AiChatRequestDTO;
import com.dhitha.lms.clientbackend.dto.AiChatResponseDTO;
import com.dhitha.lms.clientbackend.dto.UserDTO;
import com.dhitha.lms.clientbackend.service.LibraryAiAssistantService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/ai")
@PreAuthorize("hasAnyAuthority('ADMIN','USER')")
@RequiredArgsConstructor
public class ClientAiController {

  private final LibraryAiAssistantService assistantService;

  @PostMapping(
      value = "/chat",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<AiChatResponseDTO> chat(
      @Valid @RequestBody AiChatRequestDTO request, Authentication authentication) {
    return ResponseEntity.ok(assistantService.chat(request, currentUserId(authentication)));
  }

  @PostMapping(
      value = "/chat/stream",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamChat(
      @Valid @RequestBody AiChatRequestDTO request, Authentication authentication) {
    SseEmitter emitter = new SseEmitter(120_000L);
    assistantService.streamChat(request, currentUserId(authentication), emitter);
    return emitter;
  }

  private Long currentUserId(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof UserDTO)) {
      return null;
    }
    return ((UserDTO) authentication.getPrincipal()).getId();
  }
}
