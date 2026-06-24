package com.dhitha.lms.clientbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dhitha.lms.clientbackend.config.AiModelProperties;
import com.dhitha.lms.clientbackend.dto.AiChatResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class ExternalLlmClientTest {

  private AiModelProperties properties;
  private ExternalLlmClient client;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    properties = new AiModelProperties();
    properties.setApiKey("mimo-key");
    properties.setBaseUrl("http://mimo.test/v1");
    properties.setModel("mimo-model");
    properties.setLmStudioBaseUrl("http://lmstudio.test/v1");
    properties.setLmStudioModel("local-model");

    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.createServer(restTemplate);
    client = new ExternalLlmClient(properties, restTemplate, new ObjectMapper());
  }

  @Test
  void usesMimoBeforeLmStudioWhenMimoSucceeds() {
    server
        .expect(requestTo("http://mimo.test/v1/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(llmResponse("MiMo polished answer"), MediaType.APPLICATION_JSON));

    ExternalLlmClient.LlmPolishResult result = client.polish("question", localResponse());

    assertThat(result.getProvider()).isEqualTo("xiaomi-mimo");
    assertThat(result.getAnswer()).isEqualTo("MiMo polished answer");
    assertThat(result.isExternal()).isTrue();
    server.verify();
  }

  @Test
  void fallsBackToLmStudioWhenMimoFails() {
    server
        .expect(requestTo("http://mimo.test/v1/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());
    server
        .expect(requestTo("http://lmstudio.test/v1/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(llmResponse("LM Studio polished answer"), MediaType.APPLICATION_JSON));

    ExternalLlmClient.LlmPolishResult result = client.polish("question", localResponse());

    assertThat(result.getProvider()).isEqualTo("lm-studio");
    assertThat(result.getAnswer()).isEqualTo("LM Studio polished answer");
    assertThat(result.isExternal()).isTrue();
    server.verify();
  }

  @Test
  void usesLocalAnswerWhenBothExternalProvidersFail() {
    server
        .expect(requestTo("http://mimo.test/v1/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());
    server
        .expect(requestTo("http://lmstudio.test/v1/chat/completions"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());

    ExternalLlmClient.LlmPolishResult result = client.polish("question", localResponse());

    assertThat(result.getProvider()).isEqualTo("local-rule-rag-demo");
    assertThat(result.getAnswer()).isEqualTo("local answer");
    assertThat(result.isExternal()).isFalse();
    server.verify();
  }

  private AiChatResponseDTO localResponse() {
    return AiChatResponseDTO.builder().answer("local answer").build();
  }

  private String llmResponse(String answer) {
    return "{\"choices\":[{\"message\":{\"content\":\"" + answer + "\"}}]}";
  }
}
