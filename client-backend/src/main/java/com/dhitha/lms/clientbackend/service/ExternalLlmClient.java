package com.dhitha.lms.clientbackend.service;

import com.dhitha.lms.clientbackend.config.AiModelProperties;
import com.dhitha.lms.clientbackend.dto.AiChatResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalLlmClient {

  private static final String PROVIDER_MIMO = "xiaomi-mimo";
  private static final String PROVIDER_LM_STUDIO = "lm-studio";
  private static final String PROVIDER_LOCAL = "local-rule-rag-demo";

  private final AiModelProperties properties;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public boolean isConfigured() {
    return isMimoConfigured() || isLmStudioConfigured();
  }

  public LlmPolishResult polish(String question, AiChatResponseDTO localResponse) {
    if (isMimoConfigured()) {
      LlmPolishResult mimoResult =
          callOpenAiCompatibleProvider(
              PROVIDER_MIMO,
              properties.getBaseUrl(),
              properties.getModel(),
              properties.getApiKey(),
              question,
              localResponse);
      if (mimoResult.isExternal()) {
        return mimoResult;
      }
      log.warn("MiMo model call failed, trying LM Studio fallback");
    }

    if (isLmStudioConfigured()) {
      LlmPolishResult lmStudioResult =
          callOpenAiCompatibleProvider(
              PROVIDER_LM_STUDIO,
              properties.getLmStudioBaseUrl(),
              properties.getLmStudioModel(),
              null,
              question,
              localResponse);
      if (lmStudioResult.isExternal()) {
        return lmStudioResult;
      }
      log.warn("LM Studio call failed, falling back to local AI answer");
    }

    return LlmPolishResult.local(localResponse.getAnswer());
  }

  private boolean isMimoConfigured() {
    return properties.isEnabled() && StringUtils.hasText(properties.getApiKey());
  }

  private boolean isLmStudioConfigured() {
    return properties.isLmStudioEnabled() && StringUtils.hasText(properties.getLmStudioBaseUrl());
  }

  private LlmPolishResult callOpenAiCompatibleProvider(
      String provider,
      String baseUrl,
      String model,
      String apiKey,
      String question,
      AiChatResponseDTO localResponse) {
    try {
      Map<String, Object> request = new HashMap<>();
      request.put("model", model);
      request.put("temperature", 0.2);
      request.put("messages", messages(question, localResponse));

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      if (StringUtils.hasText(apiKey)) {
        headers.set("api-key", apiKey);
      }

      JsonNode response =
          restTemplate.postForObject(
              normalizeBaseUrl(baseUrl) + "/chat/completions",
              new HttpEntity<>(request, headers),
              JsonNode.class);
      JsonNode content = response == null ? null : response.at("/choices/0/message/content");
      if (content == null || content.isMissingNode() || !StringUtils.hasText(content.asText())) {
        return LlmPolishResult.local(localResponse.getAnswer());
      }
      return LlmPolishResult.external(content.asText(), provider);
    } catch (Exception e) {
      log.warn("{} model call failed: {}", provider, e.getMessage());
      return LlmPolishResult.local(localResponse.getAnswer());
    }
  }

  private List<Map<String, String>> messages(String question, AiChatResponseDTO localResponse)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    List<Map<String, String>> messages = new ArrayList<>();
    messages.add(
        message(
            "system",
            "You are the AI collection assistant in a library management system. "
                + "Answer only from the provided system tool results and RAG snippets. "
                + "Do not invent books, inventory, users, or borrowing policies. "
                + "Reply in concise Chinese for a classroom project demo."));
    messages.add(
        message(
            "user",
            "User question:\n"
                + question
                + "\nLocal business-system answer:\n"
                + localResponse.getAnswer()
                + "\nTool call records:\n"
                + objectMapper.writeValueAsString(localResponse.getToolCalls())
                + "\nKnowledge sources:\n"
                + objectMapper.writeValueAsString(localResponse.getSources())
                + "\nPlease polish this into a natural, trustworthy final answer."));
    return messages;
  }

  private String normalizeBaseUrl(String baseUrl) {
    if (baseUrl.endsWith("/")) {
      return baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl;
  }

  private Map<String, String> message(String role, String content) {
    Map<String, String> message = new HashMap<>();
    message.put("role", role);
    message.put("content", content);
    return message;
  }

  @Getter
  public static class LlmPolishResult {
    private final String answer;
    private final String provider;
    private final boolean external;

    private LlmPolishResult(String answer, String provider, boolean external) {
      this.answer = answer;
      this.provider = provider;
      this.external = external;
    }

    static LlmPolishResult external(String answer, String provider) {
      return new LlmPolishResult(answer, provider, true);
    }

    static LlmPolishResult local(String answer) {
      return new LlmPolishResult(answer, PROVIDER_LOCAL, false);
    }
  }
}
