package com.dhitha.lms.clientbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dhitha.lms.clientbackend.client.BookClient;
import com.dhitha.lms.clientbackend.client.InventoryClient;
import com.dhitha.lms.clientbackend.dto.AiChatRequestDTO;
import com.dhitha.lms.clientbackend.dto.AiChatResponseDTO;
import com.dhitha.lms.clientbackend.dto.BookDTO;
import com.dhitha.lms.clientbackend.dto.CategoryDTO;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class LibraryAiAssistantServiceTest {

  @Mock private BookClient bookClient;

  @Mock private InventoryClient inventoryClient;

  private LibraryAiAssistantService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
    service = new LibraryAiAssistantService(bookClient, inventoryClient);
  }

  @Test
  void recommendsBooksFromUserInterestAndRecordsBusinessToolCall() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull()))
        .thenReturn(
            Arrays.asList(
                book(1L, "Java Core Technology", "Java", "Cay", "Java programming guide"),
                book(2L, "Machine Learning Yearning", "AI", "Andrew", "Machine learning strategy"),
                book(3L, "Clean Code", "Software", "Robert", "Programming practice")));

    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s1", "请推荐机器学习和Java入门书"));

    assertThat(response.getIntent()).isEqualTo("BOOK_RECOMMENDATION");
    assertThat(response.getAnswer()).contains("Machine Learning Yearning", "Java Core Technology");
    assertThat(response.getToolCalls()).extracting("name").contains("BookClient.getAllBooks");
    assertThat(response.getSources()).extracting("title").contains("馆藏图书数据");
    verify(bookClient).getAllBooks(any(BookDTO.class), isNull());
  }

  @Test
  void checksBookAvailabilityWithInventoryFunctionCalling() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull()))
        .thenReturn(
            Arrays.asList(
                book(10L, "Java Core Technology", "Java", "Cay", "Java programming guide"),
                book(11L, "Clean Code", "Software", "Robert", "Programming practice")));
    when(inventoryClient.getAvailableCountOfBook(10L)).thenReturn(3L);

    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s1", "Java Core Technology 是否可借？"));

    assertThat(response.getIntent()).isEqualTo("INVENTORY_LOOKUP");
    assertThat(response.getAnswer()).contains("Java Core Technology", "可借", "3");
    assertThat(response.getToolCalls()).extracting("name").contains("InventoryClient.getAvailableCountOfBook");
    verify(inventoryClient).getAvailableCountOfBook(10L);
  }

  @Test
  void answersBorrowingRulesFromRagKnowledgeBase() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull())).thenReturn(Collections.emptyList());

    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s1", "借阅规则是什么？可以续借吗？"));

    assertThat(response.getIntent()).isEqualTo("POLICY_RAG");
    assertThat(response.getAnswer()).contains("续借", "逾期");
    assertThat(response.getSources()).extracting("title").contains("借阅与续借规则");
  }

  private BookDTO book(Long id, String name, String category, String author, String summary) {
    return BookDTO.builder()
        .id(id)
        .name(name)
        .isbn("9787111111111")
        .category(new CategoryDTO(1, category))
        .author(author)
        .publication("Demo Press")
        .pages(300)
        .publicationYear(2024)
        .summary(summary)
        .build();
  }
}
