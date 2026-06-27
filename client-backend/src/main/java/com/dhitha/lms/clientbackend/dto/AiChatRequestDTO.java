package com.dhitha.lms.clientbackend.dto;

import java.io.Serializable;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRequestDTO implements Serializable {
  private static final long serialVersionUID = 1L;

  private String sessionId;

  @NotEmpty private String message;

  private String modelProvider;

  public AiChatRequestDTO(String sessionId, String message) {
    this.sessionId = sessionId;
    this.message = message;
  }
}
