package com.dhitha.lms.clientbackend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.dhitha.lms.clientbackend.dto.AiSourceDTO;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiKnowledgeBaseServiceTest {

  @Test
  void retrievesRelevantChunksFromKnowledgeDocuments() {
    AiKnowledgeBaseService service =
        new AiKnowledgeBaseService(
            Arrays.asList(
                new AiKnowledgeBaseService.KnowledgeChunk(
                    "borrowing-policy.md", "借阅规则", "普通读者每次最多借阅 5 本，借期 30 天。"),
                new AiKnowledgeBaseService.KnowledgeChunk(
                    "renewal-policy.md", "续借规则", "到期前可续借 1 次，续借期 15 天。"),
                new AiKnowledgeBaseService.KnowledgeChunk(
                    "reservation-policy.md", "预约规则", "无可借复本时可提交预约，归还后按顺序通知。")));

    List<AiSourceDTO> sources = service.retrieve("可以续借吗？续借多久？", 2);

    assertThat(sources).isNotEmpty();
    assertThat(sources.get(0).getTitle()).contains("renewal-policy.md");
    assertThat(sources.get(0).getContent()).contains("续借期 15 天");
  }
}
