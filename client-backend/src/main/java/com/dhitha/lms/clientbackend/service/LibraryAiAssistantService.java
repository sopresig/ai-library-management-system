package com.dhitha.lms.clientbackend.service;

import com.dhitha.lms.clientbackend.client.BookClient;
import com.dhitha.lms.clientbackend.client.InventoryClient;
import com.dhitha.lms.clientbackend.client.OrderClient;
import com.dhitha.lms.clientbackend.dto.AiChatRequestDTO;
import com.dhitha.lms.clientbackend.dto.AiChatResponseDTO;
import com.dhitha.lms.clientbackend.dto.AiSourceDTO;
import com.dhitha.lms.clientbackend.dto.AiToolCallDTO;
import com.dhitha.lms.clientbackend.dto.BookDTO;
import com.dhitha.lms.clientbackend.dto.BookOrderDTO;
import com.dhitha.lms.clientbackend.dto.CategoryDTO;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class LibraryAiAssistantService {

  private static final String LOCAL_RULE_RAG_PROVIDER = "local-rule-rag-demo";
  private static final String BOOK_DATA_SOURCE = "馆藏图书数据";
  private static final int MAX_TURNS_PER_SESSION = 8;

  private final BookClient bookClient;
  private final InventoryClient inventoryClient;
  private final OrderClient orderClient;
  private final AiKnowledgeBaseService knowledgeBaseService;
  private final Map<String, AiSessionMemory> sessionMemories = new ConcurrentHashMap<>();

  @Autowired(required = false)
  private ExternalLlmClient externalLlmClient;

  public AiChatResponseDTO chat(AiChatRequestDTO request) {
    return chat(request, null);
  }

  public AiChatResponseDTO chat(AiChatRequestDTO request, Long userId) {
    AiChatResponseDTO localResponse = localChat(request, userId);
    AiChatResponseDTO response = shouldUseExternalModel(request, localResponse)
        ? withModelAnswer(request, localResponse)
        : localResponse;
    rememberTurn(request, response);
    return response;
  }

  public void streamChat(AiChatRequestDTO request, SseEmitter emitter) {
    streamChat(request, null, emitter);
  }

  public void streamChat(AiChatRequestDTO request, Long userId, SseEmitter emitter) {
    CompletableFuture.runAsync(
        () -> {
          try {
            AiChatResponseDTO localResponse = localChat(request, userId);
            StringBuilder streamedAnswer = new StringBuilder();
            ExternalLlmClient.LlmPolishResult result = null;

            if (shouldUseExternalModel(request, localResponse)
                && externalLlmClient != null
                && externalLlmClient.isConfigured()) {
              result =
                  externalLlmClient.streamPolish(
                      request.getMessage(),
                      localResponse,
                      request.getModelProvider(),
                      token -> {
                        streamedAnswer.append(token);
                        sendEvent(emitter, "token", token, MediaType.TEXT_PLAIN);
                      });
            }

            if (result == null || !result.isExternal()) {
              streamLocalAnswer(emitter, localResponse.getAnswer(), streamedAnswer);
              localResponse.setAnswer(streamedAnswer.toString());
              localResponse.setModelProvider(
                  useLocalRuleRag(request) ? LOCAL_RULE_RAG_PROVIDER : localResponse.getModelProvider());
            } else {
              localResponse.setAnswer(result.getAnswer());
              localResponse.setModelProvider(result.getProvider());
            }

            rememberTurn(request, localResponse);
            sendEvent(emitter, "done", localResponse, MediaType.APPLICATION_JSON);
            emitter.complete();
          } catch (Exception e) {
            sendEvent(emitter, "error", e.getMessage(), MediaType.TEXT_PLAIN);
            emitter.completeWithError(e);
          }
        });
  }

  private AiChatResponseDTO localChat(AiChatRequestDTO request, Long userId) {
    String sessionId = sessionId(request);
    String message = request.getMessage() == null ? "" : request.getMessage().trim();
    AiSessionMemory memory = memory(sessionId);
    List<AiToolCallDTO> toolCalls = new ArrayList<>();
    List<AiSourceDTO> sources = new ArrayList<>();

    if (isPreviousQuestionQuestion(message)) {
      return previousQuestionAnswer(sessionId, memory, toolCalls, sources);
    }

    if (isPolicyQuestion(message)) {
      return policyAnswer(sessionId, message, toolCalls, sources);
    }

    List<BookDTO> books = loadBooks(toolCalls);
    sources.add(
        AiSourceDTO.builder().title(BOOK_DATA_SOURCE).content("来自图书服务的实时馆藏基础数据").build());

    if (isBorrowIntent(message)) {
      return borrowOrderAnswer(sessionId, message, userId, books, memory, toolCalls, sources);
    }

    if (isInventoryQuestion(message)) {
      return inventoryAnswer(sessionId, message, books, memory, toolCalls, sources);
    }

    return recommendationAnswer(sessionId, message, books, memory, toolCalls, sources);
  }

  private AiChatResponseDTO withModelAnswer(AiChatRequestDTO request, AiChatResponseDTO localResponse) {
    if (useLocalRuleRag(request)) {
      localResponse.setModelProvider(LOCAL_RULE_RAG_PROVIDER);
      return localResponse;
    }
    if (externalLlmClient != null && externalLlmClient.isConfigured()) {
      ExternalLlmClient.LlmPolishResult result =
          externalLlmClient.polish(request.getMessage(), localResponse, request.getModelProvider());
      localResponse.setAnswer(result.getAnswer());
      localResponse.setModelProvider(result.getProvider());
    }
    return localResponse;
  }

  private boolean shouldUseExternalModel(AiChatRequestDTO request, AiChatResponseDTO response) {
    return !useLocalRuleRag(request)
        && !"BORROW_ORDER_CREATE".equals(response.getIntent())
        && !"BORROW_ORDER_CLARIFICATION".equals(response.getIntent())
        && !"CONTEXT_LOOKUP".equals(response.getIntent());
  }

  private boolean useLocalRuleRag(AiChatRequestDTO request) {
    return LOCAL_RULE_RAG_PROVIDER.equalsIgnoreCase(request.getModelProvider());
  }

  private void streamLocalAnswer(SseEmitter emitter, String answer, StringBuilder streamedAnswer) {
    if (answer == null) {
      return;
    }
    for (int i = 0; i < answer.length(); i++) {
      String token = String.valueOf(answer.charAt(i));
      streamedAnswer.append(token);
      sendEvent(emitter, "token", token, MediaType.TEXT_PLAIN);
    }
  }

  private void sendEvent(SseEmitter emitter, String name, Object data, MediaType mediaType) {
    try {
      emitter.send(SseEmitter.event().name(name).data(data, mediaType));
    } catch (Exception e) {
      emitter.completeWithError(e);
    }
  }

  private AiChatResponseDTO recommendationAnswer(
      String sessionId,
      String message,
      List<BookDTO> books,
      AiSessionMemory memory,
      List<AiToolCallDTO> toolCalls,
      List<AiSourceDTO> sources) {
    List<BookDTO> ranked =
        books.stream()
            .sorted(Comparator.comparingInt(book -> -scoreBook(book, message)))
            .limit(5)
            .collect(Collectors.toList());
    memory.lastBooks = ranked;

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
      AiSessionMemory memory,
      List<AiToolCallDTO> toolCalls,
      List<AiSourceDTO> sources) {
    List<BookDTO> matched =
        books.stream().filter(book -> matches(book, message)).limit(3).collect(Collectors.toList());
    if (matched.isEmpty()) {
      Integer ordinal = ordinalIndex(message);
      if (ordinal != null && ordinal >= 0 && ordinal < memory.lastBooks.size()) {
        matched = List.of(memory.lastBooks.get(ordinal));
      } else if (isContextPronoun(message) && memory.lastBooks.size() == 1) {
        matched = List.of(memory.lastBooks.get(0));
      }
    }
    if (matched.isEmpty() && !books.isEmpty()) {
      matched = books.stream().limit(3).collect(Collectors.toList());
    }
    memory.lastBooks = matched;

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
      answer.append("\n- ").append(book.getName()).append(" 当前可借 ").append(count).append(" 本。");
    }

    return response(sessionId, "INVENTORY_LOOKUP", answer.toString(), toolCalls, sources);
  }

  private AiChatResponseDTO borrowOrderAnswer(
      String sessionId,
      String message,
      Long userId,
      List<BookDTO> books,
      AiSessionMemory memory,
      List<AiToolCallDTO> toolCalls,
      List<AiSourceDTO> sources) {
    BookSelection selection = selectBookForBorrow(message, books, memory);
    if (!selection.resolved()) {
      memory.lastBooks = selection.candidates;
      return response(
          sessionId,
          "BORROW_ORDER_CLARIFICATION",
          selection.message,
          toolCalls,
          sources);
    }

    BookDTO book = selection.book;
    memory.lastBooks = List.of(book);
    if (userId == null) {
      return response(
          sessionId,
          "BORROW_ORDER_CLARIFICATION",
          "请先登录普通用户账号后再创建借书订单。",
          toolCalls,
          sources);
    }

    Long count = inventoryClient.getAvailableCountOfBook(book.getId());
    toolCalls.add(
        AiToolCallDTO.builder()
            .name("InventoryClient.getAvailableCountOfBook")
            .arguments("bookId=" + book.getId())
            .result(String.valueOf(count))
            .build());
    if (count == null || count <= 0) {
      return response(
          sessionId,
          "BORROW_ORDER_CLARIFICATION",
          "《" + book.getName() + "》当前没有可借副本，暂时不能创建借书订单。",
          toolCalls,
          sources);
    }

    try {
      BookOrderDTO orderRequest =
          BookOrderDTO.builder()
              .userId(userId)
              .bookId(book.getId())
              .bookIsbn(book.getIsbn())
              .bookName(book.getName())
              .build();
      BookOrderDTO savedOrder = orderClient.orderBook(orderRequest);
      toolCalls.add(
          AiToolCallDTO.builder()
              .name("OrderClient.orderBook")
              .arguments("userId=" + userId + ", bookId=" + book.getId())
              .result("orderId=" + savedOrder.getId())
              .build());
      String answer =
          "订单已创建："
              + "\n- 订单号："
              + savedOrder.getId()
              + "\n- 图书：《"
              + savedOrder.getBookName()
              + "》"
              + "\n- ISBN："
              + savedOrder.getBookIsbn()
              + "\n- 副本编号："
              + savedOrder.getBookReferenceId()
              + "\n请在取书截止时间前到馆领取。";
      return response(sessionId, "BORROW_ORDER_CREATE", answer, toolCalls, sources);
    } catch (Exception e) {
      return response(
          sessionId,
          "BORROW_ORDER_CLARIFICATION",
          borrowOrderFailureMessage(e, book),
          toolCalls,
          sources);
    }
  }

  private String borrowOrderFailureMessage(Exception e, BookDTO book) {
    String message = e.getMessage() == null ? "" : e.getMessage();
    if (message.contains("already has order with book")) {
      return "你已经有《" + book.getName() + "》的借书订单了，系统不会重复创建。可以到“我的订单”里查看当前订单状态。";
    }
    if (message.contains("invalid ISBN") || message.contains("无效 ISBN")) {
      return "《" + book.getName() + "》的 ISBN 数据没有通过订单服务校验，请先检查图书基础数据。";
    }
    return "暂时未能创建《" + book.getName() + "》的借书订单，请稍后重试或联系图书馆工作人员。";
  }

  private AiChatResponseDTO policyAnswer(
      String sessionId,
      String message,
      List<AiToolCallDTO> toolCalls,
      List<AiSourceDTO> sources) {
    sources.addAll(knowledgeBaseService.retrieve(message, 3));
    toolCalls.add(
        AiToolCallDTO.builder()
            .name("RAG_KNOWLEDGE_RETRIEVAL")
            .arguments("query=" + message + ", topK=3")
            .result("sources=" + sources.size())
            .build());

    String answer;
    if (sources.isEmpty()) {
      answer = "知识库暂未检索到相关规则，请补充 RAG 文档后再回答。";
    } else {
      answer =
          sources.stream().map(AiSourceDTO::getContent).collect(Collectors.joining("\n"))
              + "\n以上回答来自 RAG 知识库检索结果。";
    }
    return response(sessionId, "POLICY_RAG", answer, toolCalls, sources);
  }

  private AiChatResponseDTO previousQuestionAnswer(
      String sessionId,
      AiSessionMemory memory,
      List<AiToolCallDTO> toolCalls,
      List<AiSourceDTO> sources) {
    ConversationTurn lastTurn = memory.turns.peekLast();
    String answer =
        lastTurn == null
            ? "当前会话里还没有上一轮问题。"
            : "你刚才问的是：“" + lastTurn.question + "”。";
    return response(sessionId, "CONTEXT_LOOKUP", answer, toolCalls, sources);
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

  private BookSelection selectBookForBorrow(
      String message, List<BookDTO> books, AiSessionMemory memory) {
    Integer ordinal = ordinalIndex(message);
    if (ordinal != null) {
      if (ordinal >= 0 && ordinal < memory.lastBooks.size()) {
        return BookSelection.resolved(memory.lastBooks.get(ordinal));
      }
      return BookSelection.clarify("我没有找到你说的第 " + (ordinal + 1) + " 本，请重新指定书名。");
    }

    List<BookDTO> directMatches =
        books.stream().filter(book -> matches(book, message)).limit(3).collect(Collectors.toList());
    if (directMatches.size() == 1) {
      return BookSelection.resolved(directMatches.get(0));
    }
    if (directMatches.size() > 1) {
      return BookSelection.clarify(
          "匹配到多本图书，请说明想借哪一本："
              + directMatches.stream()
                  .map(BookDTO::getName)
                  .collect(Collectors.joining("、")));
    }

    if (isContextPronoun(message) && memory.lastBooks.size() == 1) {
      return BookSelection.resolved(memory.lastBooks.get(0));
    }
    if (isContextPronoun(message) && memory.lastBooks.size() > 1) {
      return BookSelection.clarify("你想借哪一本？可以说“借第一本”或直接输入书名。", memory.lastBooks);
    }

    return BookSelection.clarify("你想借哪一本？可以说“借第一本”或直接输入书名。");
  }

  private Integer ordinalIndex(String message) {
    String normalized = normalize(message);
    if (containsAny(normalized, "第一本", "第1本", "第 1 本", "第一", "第1")) {
      return 0;
    }
    if (containsAny(normalized, "第二本", "第2本", "第 2 本", "第二", "第2")) {
      return 1;
    }
    if (containsAny(normalized, "第三本", "第3本", "第 3 本", "第三", "第3")) {
      return 2;
    }
    if (containsAny(normalized, "第四本", "第4本", "第 4 本", "第四", "第4")) {
      return 3;
    }
    if (containsAny(normalized, "第五本", "第5本", "第 5 本", "第五", "第5")) {
      return 4;
    }
    return null;
  }

  private boolean isBorrowIntent(String message) {
    return containsAny(
        message,
        "帮我借",
        "我要借",
        "我想借",
        "就借",
        "借第一",
        "借第二",
        "借第三",
        "借第四",
        "借第五",
        "借这本",
        "借那本",
        "借它",
        "借他",
        "借这个",
        "借那个",
        "借一本",
        "创建借书订单",
        "新建借书订单",
        "生成借书订单",
        "下单",
        "预约这本",
        "预约第一",
        "办理借阅",
        "借阅这本",
        "borrow this",
        "order this book");
  }

  private boolean isContextPronoun(String message) {
    return containsAny(
        message, "这本", "那本", "这一本", "那一本", "这个", "那个", "就它", "它", "他", "借一本");
  }

  private boolean isPreviousQuestionQuestion(String message) {
    return containsAny(message, "我刚才问", "刚才我问", "上一句", "上一个问题", "前面问");
  }

  private boolean isInventoryQuestion(String message) {
    return containsAny(
        message, "可借", "能借", "借吗", "库存", "馆藏", "是否", "有吗", "available", "borrow");
  }

  private boolean isPolicyQuestion(String message) {
    return containsAny(
        message,
        "规则",
        "续借",
        "逾期",
        "预约规则",
        "借期",
        "罚款",
        "制度",
        "policy",
        "renewal",
        "overdue",
        "reservation");
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
    return haystack.contains(normalizedMessage)
        || normalizedMessage.contains(normalize(book.getName()))
        || normalizedMessage.contains(normalize(book.getIsbn()));
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
        .modelProvider(LOCAL_RULE_RAG_PROVIDER)
        .toolCalls(toolCalls)
        .sources(sources)
        .build();
  }

  private void rememberTurn(AiChatRequestDTO request, AiChatResponseDTO response) {
    String question = request.getMessage() == null ? "" : request.getMessage().trim();
    if (question.isEmpty()) {
      return;
    }
    AiSessionMemory memory = memory(sessionId(request));
    memory.turns.addLast(new ConversationTurn(question, response.getAnswer()));
    while (memory.turns.size() > MAX_TURNS_PER_SESSION) {
      memory.turns.removeFirst();
    }
  }

  private AiSessionMemory memory(String sessionId) {
    return sessionMemories.computeIfAbsent(sessionId, ignored -> new AiSessionMemory());
  }

  private String sessionId(AiChatRequestDTO request) {
    String sessionId = request.getSessionId();
    return sessionId == null || sessionId.trim().isEmpty() ? "default-session" : sessionId.trim();
  }

  private String normalize(String value) {
    return value == null
        ? ""
        : value.toLowerCase(Locale.ROOT).replaceAll("[，,。！!？?：:；;《》\"'（）()]", " ").trim();
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static class AiSessionMemory {
    private final Deque<ConversationTurn> turns = new ArrayDeque<>();
    private List<BookDTO> lastBooks = List.of();
  }

  private static class ConversationTurn {
    private final String question;
    private final String answer;

    private ConversationTurn(String question, String answer) {
      this.question = question;
      this.answer = answer;
    }
  }

  private static class BookSelection {
    private final BookDTO book;
    private final String message;
    private final List<BookDTO> candidates;

    private BookSelection(BookDTO book, String message, List<BookDTO> candidates) {
      this.book = book;
      this.message = message;
      this.candidates = candidates;
    }

    private static BookSelection resolved(BookDTO book) {
      return new BookSelection(book, null, List.of(book));
    }

    private static BookSelection clarify(String message) {
      return new BookSelection(null, message, List.of());
    }

    private static BookSelection clarify(String message, List<BookDTO> candidates) {
      return new BookSelection(null, message, candidates);
    }

    private boolean resolved() {
      return book != null;
    }
  }
}
