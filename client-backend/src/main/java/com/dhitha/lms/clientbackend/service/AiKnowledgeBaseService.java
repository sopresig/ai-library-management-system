package com.dhitha.lms.clientbackend.service;

import com.dhitha.lms.clientbackend.dto.AiSourceDTO;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiKnowledgeBaseService {

  private static final String KNOWLEDGE_PATTERN = "classpath*:rag/*.md";

  private final ExternalEmbeddingClient embeddingClient;
  private volatile List<KnowledgeChunk> chunks;

  public AiKnowledgeBaseService() {
    this(null, null);
  }

  @Autowired
  public AiKnowledgeBaseService(ExternalEmbeddingClient embeddingClient) {
    this(null, embeddingClient);
  }

  AiKnowledgeBaseService(List<KnowledgeChunk> chunks) {
    this(chunks, null);
  }

  AiKnowledgeBaseService(List<KnowledgeChunk> chunks, ExternalEmbeddingClient embeddingClient) {
    this.chunks = chunks;
    this.embeddingClient = embeddingClient;
  }

  public List<AiSourceDTO> retrieve(String query, int limit) {
    if (!StringUtils.hasText(query) || limit <= 0) {
      return new ArrayList<>();
    }

    List<AiSourceDTO> embeddingResult = retrieveByEmbeddings(query, limit);
    if (!embeddingResult.isEmpty()) {
      return embeddingResult;
    }

    return retrieveByLexical(query, limit);
  }

  private List<AiSourceDTO> retrieveByEmbeddings(String query, int limit) {
    if (embeddingClient == null || !embeddingClient.isConfigured()) {
      return new ArrayList<>();
    }

    Optional<double[]> queryEmbedding = embeddingClient.embed(query);
    if (!queryEmbedding.isPresent()) {
      return new ArrayList<>();
    }

    return chunks()
        .stream()
        .map(chunk -> scoreByEmbedding(chunk, queryEmbedding.get()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(scored -> scored.score > 0)
        .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
        .limit(limit)
        .map(this::toSource)
        .collect(Collectors.toList());
  }

  private Optional<ScoredChunk> scoreByEmbedding(KnowledgeChunk chunk, double[] queryEmbedding) {
    Optional<double[]> chunkEmbedding = embeddingFor(chunk);
    if (!chunkEmbedding.isPresent()) {
      return Optional.empty();
    }
    return Optional.of(new ScoredChunk(chunk, cosine(queryEmbedding, chunkEmbedding.get())));
  }

  private Optional<double[]> embeddingFor(KnowledgeChunk chunk) {
    double[] cachedEmbedding = chunk.embedding;
    if (cachedEmbedding != null) {
      return Optional.of(cachedEmbedding);
    }

    synchronized (chunk) {
      if (chunk.embedding != null) {
        return Optional.of(chunk.embedding);
      }
      Optional<double[]> generatedEmbedding = embeddingClient.embed(chunk.searchText());
      generatedEmbedding.ifPresent(chunk::setEmbedding);
      return generatedEmbedding;
    }
  }

  private List<AiSourceDTO> retrieveByLexical(String query, int limit) {
    Map<String, Integer> queryVector = vectorize(query);
    return chunks()
        .stream()
        .map(chunk -> new ScoredChunk(chunk, cosine(queryVector, vectorize(chunk.searchText()))))
        .filter(scored -> scored.score > 0)
        .sorted(Comparator.comparingDouble(ScoredChunk::getScore).reversed())
        .limit(limit)
        .map(this::toSource)
        .collect(Collectors.toList());
  }

  private AiSourceDTO toSource(ScoredChunk scored) {
    return AiSourceDTO.builder()
        .title(scored.chunk.sourceTitle())
        .content(scored.chunk.content)
        .build();
  }

  private List<KnowledgeChunk> chunks() {
    if (chunks == null) {
      synchronized (this) {
        if (chunks == null) {
          chunks = loadChunks();
        }
      }
    }
    return chunks;
  }

  private List<KnowledgeChunk> loadChunks() {
    List<KnowledgeChunk> loaded = new ArrayList<>();
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    try {
      for (Resource resource : resolver.getResources(KNOWLEDGE_PATTERN)) {
        loaded.addAll(readResource(resource));
      }
    } catch (Exception e) {
      return loaded;
    }
    return loaded;
  }

  private List<KnowledgeChunk> readResource(Resource resource) throws java.io.IOException {
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    }

    String documentName = resource.getFilename() == null ? "knowledge.md" : resource.getFilename();
    String sectionTitle = documentName;
    List<KnowledgeChunk> result = new ArrayList<>();
    StringBuilder paragraph = new StringBuilder();

    for (String line : lines) {
      String trimmed = line.trim();
      if (trimmed.startsWith("#")) {
        addParagraph(result, documentName, sectionTitle, paragraph);
        sectionTitle = trimmed.replaceFirst("^#+", "").trim();
      } else if (trimmed.isEmpty()) {
        addParagraph(result, documentName, sectionTitle, paragraph);
      } else {
        if (paragraph.length() > 0) {
          paragraph.append('\n');
        }
        paragraph.append(trimmed);
      }
    }
    addParagraph(result, documentName, sectionTitle, paragraph);
    return result;
  }

  private void addParagraph(
      List<KnowledgeChunk> chunks, String documentName, String sectionTitle, StringBuilder paragraph) {
    if (paragraph.length() == 0) {
      return;
    }
    chunks.add(new KnowledgeChunk(documentName, sectionTitle, paragraph.toString()));
    paragraph.setLength(0);
  }

  private Map<String, Integer> vectorize(String text) {
    Map<String, Integer> vector = new HashMap<>();
    List<String> chineseChars = new ArrayList<>();
    StringBuilder word = new StringBuilder();

    for (int i = 0; i < text.length(); i++) {
      char c = Character.toLowerCase(text.charAt(i));
      if (Character.isLetterOrDigit(c) && !isChinese(c)) {
        word.append(c);
        flushChineseTokens(chineseChars, vector);
      } else {
        flushWord(word, vector);
        if (isChinese(c)) {
          chineseChars.add(String.valueOf(c));
        } else {
          flushChineseTokens(chineseChars, vector);
        }
      }
    }
    flushWord(word, vector);
    flushChineseTokens(chineseChars, vector);
    return vector;
  }

  private void flushWord(StringBuilder word, Map<String, Integer> vector) {
    if (word.length() >= 2) {
      addToken(vector, word.toString());
    }
    word.setLength(0);
  }

  private void flushChineseTokens(List<String> chars, Map<String, Integer> vector) {
    for (String c : chars) {
      addToken(vector, c);
    }
    for (int i = 0; i + 1 < chars.size(); i++) {
      addToken(vector, chars.get(i) + chars.get(i + 1));
    }
    chars.clear();
  }

  private void addToken(Map<String, Integer> vector, String token) {
    vector.put(token, vector.getOrDefault(token, 0) + 1);
  }

  private boolean isChinese(char c) {
    return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
  }

  private double cosine(Map<String, Integer> left, Map<String, Integer> right) {
    if (left.isEmpty() || right.isEmpty()) {
      return 0;
    }

    double dot = 0;
    for (Map.Entry<String, Integer> entry : left.entrySet()) {
      dot += entry.getValue() * right.getOrDefault(entry.getKey(), 0);
    }
    return dot / (magnitude(left) * magnitude(right));
  }

  private double cosine(double[] left, double[] right) {
    if (left.length == 0 || right.length == 0 || left.length != right.length) {
      return 0;
    }

    double dot = 0;
    double leftMagnitude = 0;
    double rightMagnitude = 0;
    for (int i = 0; i < left.length; i++) {
      dot += left[i] * right[i];
      leftMagnitude += left[i] * left[i];
      rightMagnitude += right[i] * right[i];
    }
    if (leftMagnitude == 0 || rightMagnitude == 0) {
      return 0;
    }
    return dot / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
  }

  private double magnitude(Map<String, Integer> vector) {
    double sum = 0;
    for (Integer value : vector.values()) {
      sum += value * value;
    }
    return Math.sqrt(sum);
  }

  public static class KnowledgeChunk {
    private final String documentName;
    private final String sectionTitle;
    private final String content;
    private volatile double[] embedding;

    public KnowledgeChunk(String documentName, String sectionTitle, String content) {
      this.documentName = documentName;
      this.sectionTitle = sectionTitle;
      this.content = content;
    }

    private String sourceTitle() {
      return documentName + " - " + sectionTitle;
    }

    private String searchText() {
      return String.format(Locale.ROOT, "%s\n%s\n%s", documentName, sectionTitle, content);
    }

    private void setEmbedding(double[] embedding) {
      this.embedding = embedding;
    }
  }

  private static class ScoredChunk {
    private final KnowledgeChunk chunk;
    private final double score;

    private ScoredChunk(KnowledgeChunk chunk, double score) {
      this.chunk = chunk;
      this.score = score;
    }

    private double getScore() {
      return score;
    }
  }
}
