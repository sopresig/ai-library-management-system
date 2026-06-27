package com.dhitha.lms.clientbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dhitha.lms.clientbackend.dto.AiSourceDTO;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AiKnowledgeBaseEmbeddingTest {

  @Test
  void retrievesChunksByEmbeddingSimilarityWhenEmbeddingClientIsConfigured() {
    ExternalEmbeddingClient embeddingClient = mock(ExternalEmbeddingClient.class);
    when(embeddingClient.isConfigured()).thenReturn(true);
    when(embeddingClient.embed(anyString()))
        .thenAnswer(
            invocation -> {
              String text = invocation.getArgument(0);
              if ("renewal question".equals(text)) {
                return Optional.of(new double[] {1.0, 0.0});
              }
              if (text.contains("renew")) {
                return Optional.of(new double[] {0.95, 0.05});
              }
              return Optional.of(new double[] {0.0, 1.0});
            });

    AiKnowledgeBaseService service =
        new AiKnowledgeBaseService(
            Arrays.asList(
                new AiKnowledgeBaseService.KnowledgeChunk(
                    "reservation-policy.md", "reservation", "place a hold when no copy is available"),
                new AiKnowledgeBaseService.KnowledgeChunk(
                    "renewal-policy.md", "renewal", "renew before due date for 15 more days")),
            embeddingClient);

    List<AiSourceDTO> sources = service.retrieve("renewal question", 1);

    assertThat(sources).hasSize(1);
    assertThat(sources.get(0).getTitle()).contains("renewal-policy.md");
    verify(embeddingClient).embed("renewal question");
  }
}
