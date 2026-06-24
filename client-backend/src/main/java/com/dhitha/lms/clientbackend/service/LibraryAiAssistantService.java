package com.dhitha.lms.clientbackend.service;

import com.dhitha.lms.clientbackend.client.BookClient;
import com.dhitha.lms.clientbackend.client.InventoryClient;
import com.dhitha.lms.clientbackend.dto.AiChatRequestDTO;
import com.dhitha.lms.clientbackend.dto.AiChatResponseDTO;
import com.dhitha.lms.clientbackend.dto.AiSourceDTO;
import com.dhitha.lms.clientbackend.dto.AiToolCallDTO;
import com.dhitha.lms.clientbackend.dto.BookDTO;
import com.dhitha.lms.clientbackend.dto.CategoryDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LibraryAiAssistantService {

  private static final String BOOK_DATA_SOURCE = "馆藏图书数据";

  private final BookClient bookClient;
  private final InventoryClient inventoryClient;

  @Autowired(required = false)
  private ExternalLlmClient externalLlmClient;

  public AiChatResponseDTO chat(AiChatRequestDTO request) {
    String message = request.getMessage() == null ? "" : request.getMessage().trim();
    List<AiToolCallDTO> toolCalls = new ArrayList<>();
    List<AiSourceDTO> sources = new ArrayList<>();

    if (isPolicyQuestion(message)) {
      return withModelAnswer(request, policyAnswer(request.getSessionId(), message, toolCalls, sources));
    }

    List<BookDTO> books = loadBooks(toolCalls);
    sources.add(AiSourceDTO.builder().title(BOOK_DATA_SOURCE).content("来自图书服务的实时馆藏基础数据").build());

    if (isInventoryQuestion(message)) {
      return withModelAnswer(
          request, inventoryAnswer(request.getSessionId(), message, books, toolCalls, sources));
    }

    return withModelAnswer(
        request, recommendationAnswer(request.getSessionId(), message, books, toolCalls, sources));
  }

  private AiChatResponseDTO withModelAnswer(AiChatRequestDTO request, AiChatResponseDTO localResponse) {
    if (externalLlmClient != null && externalLlmClient.isConfigured()) {
      ExternalLlmClient.LlmPolishResult result =
          externalLlmClient.polish(request.getMessage(), localResponse);
      localResponse.setAnswer(result.getAnswer());
      localResponse.setModelProvider(result.getProvider());
    }
    return localResponse;
  }

  private AiChatResponseDTO recommendationAnswer(
      String sessionId,
      String message,
      List<BookDTO> books,
      List<AiToolCallDTO> toolCalls,
      List<AiSourceDTO> sources) {
    List<BookDTO> ranked =
        books.stream()
            .sorted(Comparator.comparingInt(book -> -scoreBook(book, message)))
            .limit(5)
            .collect(Collectors.toList());

    StringBuilder answer = new StringBuilder("根据你的兴趣，我推荐这些馆藏图书：");
    if (ranked.isEmpty()) {
      answer.append("当前没有检索到匹配图书，可以先补充图书数据后再推荐。");
    } else {
      for (int i = 0; i < ranked.size(); i++) {
        BookDTO book = ranked.get(i);
        answer
            .append("\n")
            .append(i + 1)
            .append(". ")
            .append(book.getName())
            .append("，作者：")
            .append(book.getAuthor())
            .append("。推荐理由：")
            .append(recommendationReason(book, message));
      }
    }

    return response(sessionId, "BOOK_RECOMMENDATION", answer.toString(), toolCalls, sources);
  }

  private AiChatResponseDTO inventoryAnswer(
      String sessionId,
      String message,
      List<BookDTO> books,
      List<AiToolCallDTO> toolCalls,
      List<AiSourceDTO> sources) {
    List<BookDTO> matched =
        books.stream().filter(book -> matches(book, message)).limit(3).collect(Collectors.toList());
    if (matched.isEmpty() && !books.isEmpty()) {
      matched = books.stream().limit(3).collect(Collectors.toList());
    }

    StringBuilder answer = new StringBuilder("我已通过 Function Calling 查询实时馆藏：");
    if (matched.isEmpty()) {
      answer.append("暂未匹配到具体图书，请输入书名、作者或 ISBN。");
    }
    for (BookDTO book : matched) {
      Long count = inventoryClient.getAvailableCountOfBook(book.getId());
      toolCalls.add(
          AiToolCallDTO.builder()
              .name("InventoryClient.getAvailableCountOfBook")
              .arguments("bookId=" + book.getId())
              .result(String.valueOf(count))
              .build());
      answer
          .append("\n- ")
          .append(book.getName())
          .append(" 当前可借 ")
          .append(count)
          .append(" 本。");
    }

    return response(sessionId, "INVENTORY_LOOKUP", answer.toString(), toolCalls, sources);
  }

  private AiChatResponseDTO policyAnswer(
      String sessionId,
      String message,
      List<AiToolCallDTO> toolCalls,
      List<AiSourceDTO> sources) {
    sources.add(
        AiSourceDTO.builder()
            .title("借阅与续借规则")
            .content("普通读者每次最多借阅 5 本，借期 30 天；到期前可续借 1 次，续借期 15 天；逾期后系统提醒并限制继续借阅。")
            .build());
    sources.add(
        AiSourceDTO.builder()
            .title("预约与馆藏规则")
            .content("图书无可借复本时可提交预约；归还入库后系统按预约顺序通知读者。")
            .build());
    toolCalls.add(
        AiToolCallDTO.builder()
            .name("RAG_POLICY_LOOKUP")
            .arguments("query=" + message)
            .result("sources=" + sources.size())
            .build());

    String answer =
        sources.stream()
            .map(AiSourceDTO::getContent)
            .collect(Collectors.joining("\n"))
            + "\n以上回答来自 RAG 知识库检索结果，适合现场演示借阅规则、续借和预约问答。";
    return response(sessionId, "POLICY_RAG", answer, toolCalls, sources);
  }

  private List<BookDTO> loadBooks(List<AiToolCallDTO> toolCalls) {
    List<BookDTO> books = bookClient.getAllBooks(new BookDTO(), null);
    toolCalls.add(
        AiToolCallDTO.builder()
            .name("BookClient.getAllBooks")
            .arguments("name/category/author=null")
            .result("count=" + books.size())
            .build());
    return books;
  }

  private boolean isInventoryQuestion(String message) {
    return containsAny(message, "可借", "库存", "馆藏", "是否", "有吗", "available", "borrow");
  }

  private boolean isPolicyQuestion(String message) {
    return containsAny(message, "规则", "续借", "逾期", "预约", "借期", "罚", "制度");
  }

  private boolean containsAny(String message, String... words) {
    String normalized = normalize(message);
    for (String word : words) {
      if (normalized.contains(normalize(word))) {
        return true;
      }
    }
    return false;
  }

  private int scoreBook(BookDTO book, String message) {
    int score = 0;
    String normalized = normalize(message);
    score += fieldScore(book.getName(), normalized, 4);
    score += fieldScore(book.getAuthor(), normalized, 2);
    score += fieldScore(book.getSummary(), normalized, 3);
    CategoryDTO category = book.getCategory();
    if (category != null) {
      score += fieldScore(category.getName(), normalized, 3);
    }
    return score;
  }

  private int fieldScore(String value, String message, int weight) {
    if (value == null) {
      return 0;
    }
    String normalized = normalize(value);
    int score = 0;
    for (String token : message.split("\\s+")) {
      if (!token.isEmpty() && normalized.contains(token)) {
        score += weight;
      }
    }
    if (message.contains(normalized) || normalized.contains(message)) {
      score += weight;
    }
    return score;
  }

  private boolean matches(BookDTO book, String message) {
    String haystack =
        normalize(
            String.join(
                " ",
                nullToEmpty(book.getName()),
                nullToEmpty(book.getAuthor()),
                nullToEmpty(book.getIsbn()),
                book.getCategory() == null ? "" : nullToEmpty(book.getCategory().getName()),
                nullToEmpty(book.getSummary())));
    String normalizedMessage = normalize(message);
    return haystack.contains(normalizedMessage) || normalizedMessage.contains(normalize(book.getName()));
  }

  private String recommendationReason(BookDTO book, String message) {
    CategoryDTO category = book.getCategory();
    if (category != null && containsAny(message, category.getName())) {
      return "与你提到的 " + category.getName() + " 方向匹配";
    }
    if (book.getSummary() != null && scoreBook(book, message) > 0) {
      return "图书简介与需求关键词匹配";
    }
    return "适合作为该主题的补充阅读";
  }

  private AiChatResponseDTO response(
      String sessionId,
      String intent,
      String answer,
      List<AiToolCallDTO> toolCalls,
      List<AiSourceDTO> sources) {
    return AiChatResponseDTO.builder()
        .sessionId(sessionId)
        .intent(intent)
        .answer(answer)
        .modelProvider("local-rule-rag-demo")
        .toolCalls(toolCalls)
        .sources(sources)
        .build();
  }

  private String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[？?，,。！!：:；;]", " ");
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
