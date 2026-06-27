package com.dhitha.lms.clientbackend.service;

import com.dhitha.lms.clientbackend.config.AiModelProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
public class ExternalEmbeddingClient {

  private final AiModelProperties properties;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public boolean isConfigured() {
    return properties.isEmbeddingEnabled()
        && StringUtils.hasText(properties.getEmbeddingBaseUrl())
        && StringUtils.hasText(properties.getEmbeddingModel());
  }

  public Optional<double[]> embed(String input) {
    if (!isConfigured() || !StringUtils.hasText(input)) {
      return Optional.empty();
    }

    try {
      Map<String, Object> request = new HashMap<>();
      request.put("model", properties.getEmbeddingModel());
      request.put("input", input);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      if (StringUtils.hasText(properties.getEmbeddingApiKey())) {
        headers.set("api-key", properties.getEmbeddingApiKey());
        headers.setBearerAuth(properties.getEmbeddingApiKey());
      }

      JsonNode response =
          restTemplate.postForObject(
              normalizeBaseUrl(properties.getEmbeddingBaseUrl()) + "/embeddings",
              new HttpEntity<>(request, headers),
              JsonNode.class);
      return parseEmbedding(response);
    } catch (Exception e) {
      log.warn("Embedding model call failed: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private Optional<double[]> parseEmbedding(JsonNode response) {
    JsonNode embedding = response == null ? null : response.at("/data/0/embedding");
    if (embedding == null || embedding.isMissingNode() || !embedding.isArray()) {
      return Optional.empty();
    }

    double[] vector = objectMapper.convertValue(embedding, double[].class);
    if (vector.length == 0) {
      return Optional.empty();
    }
    return Optional.of(vector);
  }

  private String normalizeBaseUrl(String baseUrl) {
    if (baseUrl.endsWith("/")) {
      return baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl;
  }
}
