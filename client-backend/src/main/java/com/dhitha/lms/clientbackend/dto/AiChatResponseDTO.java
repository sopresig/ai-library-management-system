package com.dhitha.lms.clientbackend.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponseDTO implements Serializable {
  private static final long serialVersionUID = 1L;

  private String sessionId;
  private String intent;
  private String answer;
  private String modelProvider;

  @Builder.Default private List<AiToolCallDTO> toolCalls = new ArrayList<>();

  @Builder.Default private List<AiSourceDTO> sources = new ArrayList<>();
}
