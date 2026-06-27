package com.dhitha.lms.clientbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.dhitha.lms.clientbackend.config.AiModelProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class ExternalEmbeddingClientTest {

  private AiModelProperties properties;
  private ExternalEmbeddingClient client;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    properties = new AiModelProperties();
    properties.setEmbeddingBaseUrl("http://lmstudio.test/v1");
    properties.setEmbeddingModel("text-embedding-nomic-embed-text-v1.5@q8_0");
    properties.setEmbeddingEnabled(true);

    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.createServer(restTemplate);
    client = new ExternalEmbeddingClient(properties, restTemplate, new ObjectMapper());
  }

  @Test
  void callsOpenAiCompatibleEmbeddingsApiAndParsesVector() {
    server
        .expect(requestTo("http://lmstudio.test/v1/embeddings"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"text-embedding-nomic-embed-text-v1.5@q8_0\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"input\":\"renewal policy\"")))
        .andRespond(
            withSuccess(
                "{\"data\":[{\"embedding\":[0.1,0.2,0.3]}]}",
                MediaType.APPLICATION_JSON));

    Optional<double[]> embedding = client.embed("renewal policy");

    assertThat(embedding).isPresent();
    assertThat(embedding.get()).containsExactly(0.1, 0.2, 0.3);
    server.verify();
  }

  @Test
  void returnsEmptyWhenEmbeddingProviderIsDisabled() {
    properties.setEmbeddingEnabled(false);

    Optional<double[]> embedding = client.embed("renewal policy");

    assertThat(embedding).isEmpty();
  }
}
