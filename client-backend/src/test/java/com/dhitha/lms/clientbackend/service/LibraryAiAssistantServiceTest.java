package com.dhitha.lms.clientbackend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dhitha.lms.clientbackend.client.BookClient;
import com.dhitha.lms.clientbackend.client.InventoryClient;
import com.dhitha.lms.clientbackend.client.OrderClient;
import com.dhitha.lms.clientbackend.dto.AiChatRequestDTO;
import com.dhitha.lms.clientbackend.dto.AiChatResponseDTO;
import com.dhitha.lms.clientbackend.dto.AiSourceDTO;
import com.dhitha.lms.clientbackend.dto.BookOrderDTO;
import com.dhitha.lms.clientbackend.dto.BookDTO;
import com.dhitha.lms.clientbackend.dto.CategoryDTO;
import java.util.Arrays;
import java.util.Collections;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class LibraryAiAssistantServiceTest {

  @Mock private BookClient bookClient;

  @Mock private InventoryClient inventoryClient;

  @Mock private OrderClient orderClient;

  @Mock private AiKnowledgeBaseService knowledgeBaseService;

  private LibraryAiAssistantService service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.initMocks(this);
    service = new LibraryAiAssistantService(
        bookClient, inventoryClient, orderClient, knowledgeBaseService);
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
        service.chat(new AiChatRequestDTO("s1", "Recommend machine learning and Java books"));

    assertThat(response.getIntent()).isEqualTo("BOOK_RECOMMENDATION");
    assertThat(response.getAnswer()).contains("Machine Learning Yearning", "Java Core Technology");
    assertThat(response.getToolCalls()).extracting("name").contains("BookClient.getAllBooks");
    verify(bookClient).getAllBooks(any(BookDTO.class), isNull());
  }

  @Test
  void localRuleRagProviderKeepsLocalAnswer() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull()))
        .thenReturn(
            Collections.singletonList(
                book(1L, "Java Core Technology", "Java", "Cay", "Java programming guide")));

    AiChatResponseDTO response =
        service.chat(
            new AiChatRequestDTO("s1", "Recommend Java books", "local-rule-rag-demo"));

    assertThat(response.getModelProvider()).isEqualTo("local-rule-rag-demo");
    assertThat(response.getAnswer()).contains("Java Core Technology");
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
        service.chat(new AiChatRequestDTO("s1", "Java Core Technology available to borrow?"));

    assertThat(response.getIntent()).isEqualTo("INVENTORY_LOOKUP");
    assertThat(response.getAnswer()).contains("Java Core Technology", "3");
    assertThat(response.getToolCalls())
        .extracting("name")
        .contains("InventoryClient.getAvailableCountOfBook");
    verify(inventoryClient).getAvailableCountOfBook(10L);
  }

  @Test
  void answersBorrowingRulesFromRetrievedRagKnowledgeBase() {
    when(knowledgeBaseService.retrieve("借阅规则是什么？可以续借吗？", 3))
        .thenReturn(
            Collections.singletonList(
                AiSourceDTO.builder()
                    .title("renewal-policy.md - 续借规则")
                    .content("到期前可续借 1 次，续借期 15 天；逾期后系统限制继续借阅。")
                    .build()));

    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s1", "借阅规则是什么？可以续借吗？"));

    assertThat(response.getIntent()).isEqualTo("POLICY_RAG");
    assertThat(response.getAnswer()).contains("续借期 15 天", "逾期");
    assertThat(response.getSources()).extracting("title").contains("renewal-policy.md - 续借规则");
    assertThat(response.getToolCalls()).extracting("name").contains("RAG_KNOWLEDGE_RETRIEVAL");
    verify(knowledgeBaseService).retrieve("借阅规则是什么？可以续借吗？", 3);
  }

  @Test
  void doesNotCreateBorrowOrderWhenUserOnlyAsksAvailability() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull()))
        .thenReturn(
            Collections.singletonList(
                book(10L, "Java Core Technology", "Java", "Cay", "Java programming guide")));
    when(inventoryClient.getAvailableCountOfBook(10L)).thenReturn(2L);

    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s1", "Java Core Technology 能借吗？"), 2L);

    assertThat(response.getIntent()).isEqualTo("INVENTORY_LOOKUP");
    assertThat(response.getAnswer()).contains("Java Core Technology", "2");
    verifyNoInteractions(orderClient);
  }

  @Test
  void createsBorrowOrderWhenUserClearlyAsksToBorrowNamedBook() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull()))
        .thenReturn(
            Collections.singletonList(
                book(10L, "Java Core Technology", "Java", "Cay", "Java programming guide")));
    when(inventoryClient.getAvailableCountOfBook(10L)).thenReturn(2L);
    when(orderClient.orderBook(any(BookOrderDTO.class)))
        .thenReturn(
            BookOrderDTO.builder()
                .id(99L)
                .bookId(10L)
                .bookIsbn("9787111111111")
                .bookName("Java Core Technology")
                .bookReferenceId("10b-1c-1")
                .build());

    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s1", "帮我借 Java Core Technology"), 2L);

    assertThat(response.getIntent()).isEqualTo("BORROW_ORDER_CREATE");
    assertThat(response.getAnswer()).contains("订单已创建", "Java Core Technology", "99");
    assertThat(response.getToolCalls()).extracting("name").contains("OrderClient.orderBook");

    ArgumentCaptor<BookOrderDTO> orderCaptor = ArgumentCaptor.forClass(BookOrderDTO.class);
    verify(orderClient).orderBook(orderCaptor.capture());
    assertThat(orderCaptor.getValue().getUserId()).isEqualTo(2L);
    assertThat(orderCaptor.getValue().getBookId()).isEqualTo(10L);
  }

  @Test
  void createsBorrowOrderFromOrdinalReferenceInConversationContext() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull()))
        .thenReturn(
            Arrays.asList(
                book(10L, "Java Core Technology", "Java", "Cay", "Java programming guide"),
                book(11L, "Clean Code", "Software", "Robert", "Programming practice")));
    when(inventoryClient.getAvailableCountOfBook(10L)).thenReturn(1L);
    when(orderClient.orderBook(any(BookOrderDTO.class)))
        .thenReturn(
            BookOrderDTO.builder()
                .id(100L)
                .bookId(10L)
                .bookIsbn("9787111111111")
                .bookName("Java Core Technology")
                .bookReferenceId("10b-1c-1")
                .build());

    service.chat(new AiChatRequestDTO("s-context", "推荐 Java 入门书"), 2L);
    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s-context", "就借第一本"), 2L);

    assertThat(response.getIntent()).isEqualTo("BORROW_ORDER_CREATE");
    assertThat(response.getAnswer()).contains("Java Core Technology", "订单已创建");
    ArgumentCaptor<BookOrderDTO> orderCaptor = ArgumentCaptor.forClass(BookOrderDTO.class);
    verify(orderClient).orderBook(orderCaptor.capture());
    assertThat(orderCaptor.getValue().getBookId()).isEqualTo(10L);
  }

  @Test
  void createsBorrowOrderFromPronounReferenceAfterInventoryLookup() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull()))
        .thenReturn(
            Collections.singletonList(
                book(11L, "Clean Code", "Software", "Robert", "Programming practice")));
    when(inventoryClient.getAvailableCountOfBook(11L)).thenReturn(1L);
    when(orderClient.orderBook(any(BookOrderDTO.class)))
        .thenReturn(
            BookOrderDTO.builder()
                .id(101L)
                .bookId(11L)
                .bookIsbn("9787111111111")
                .bookName("Clean Code")
                .bookReferenceId("11b-1c-1")
                .build());

    service.chat(new AiChatRequestDTO("s-pronoun-borrow", "Clean Code 能借吗？"), 2L);
    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s-pronoun-borrow", "借他"), 2L);

    assertThat(response.getIntent()).isEqualTo("BORROW_ORDER_CREATE");
    assertThat(response.getAnswer()).contains("Clean Code");
    ArgumentCaptor<BookOrderDTO> orderCaptor = ArgumentCaptor.forClass(BookOrderDTO.class);
    verify(orderClient).orderBook(orderCaptor.capture());
    assertThat(orderCaptor.getValue().getBookId()).isEqualTo(11L);
  }

  @Test
  void showsFriendlyMessageWhenBorrowOrderAlreadyExists() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull()))
        .thenReturn(
            Collections.singletonList(
                book(10L, "Java Core Technology", "Java", "Cay", "Java programming guide")));
    when(inventoryClient.getAvailableCountOfBook(10L)).thenReturn(1L);
    when(orderClient.orderBook(any(BookOrderDTO.class)))
        .thenThrow(new RuntimeException("User with id 2 already has order with book 10"));

    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s-duplicate-order", "帮我借 Java Core Technology"), 2L);

    assertThat(response.getIntent()).isEqualTo("BORROW_ORDER_CLARIFICATION");
    assertThat(response.getAnswer()).contains("已经有", "Java Core Technology");
    assertThat(response.getAnswer()).doesNotContain("RuntimeException", "Feign", "POST");
  }

  @Test
  void checksInventoryFromOrdinalReferenceInConversationContextWithoutCreatingOrder() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull()))
        .thenReturn(
            Arrays.asList(
                book(11L, "Clean Code", "Software", "Robert", "Programming practice"),
                book(10L, "Java Core Technology", "Java", "Cay", "Java programming guide")));
    when(inventoryClient.getAvailableCountOfBook(10L)).thenReturn(2L);

    service.chat(new AiChatRequestDTO("s-inventory-context", "推荐 Java 入门书"), 2L);
    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s-inventory-context", "第一本能借吗？"), 2L);

    assertThat(response.getIntent()).isEqualTo("INVENTORY_LOOKUP");
    assertThat(response.getAnswer()).contains("Java Core Technology", "2");
    assertThat(response.getAnswer()).doesNotContain("Clean Code");
    verifyNoInteractions(orderClient);
  }

  @Test
  void asksForClarificationWhenBorrowIntentCannotIdentifyOneBook() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull()))
        .thenReturn(
            Arrays.asList(
                book(10L, "Java Core Technology", "Java", "Cay", "Java programming guide"),
                book(11L, "Clean Code", "Software", "Robert", "Programming practice")));

    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s1", "帮我借这本"), 2L);

    assertThat(response.getIntent()).isEqualTo("BORROW_ORDER_CLARIFICATION");
    assertThat(response.getAnswer()).contains("想借哪一本");
    verifyNoInteractions(orderClient);
  }

  @Test
  void answersPreviousQuestionFromConversationContext() {
    when(bookClient.getAllBooks(any(BookDTO.class), isNull()))
        .thenReturn(
            Collections.singletonList(
                book(10L, "Java Core Technology", "Java", "Cay", "Java programming guide")));

    service.chat(new AiChatRequestDTO("s-history", "推荐 Java 入门书"), 2L);
    AiChatResponseDTO response =
        service.chat(new AiChatRequestDTO("s-history", "我刚才问了什么"), 2L);

    assertThat(response.getIntent()).isEqualTo("CONTEXT_LOOKUP");
    assertThat(response.getAnswer()).contains("推荐 Java 入门书");
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
