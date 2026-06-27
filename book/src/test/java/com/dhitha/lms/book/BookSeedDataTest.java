package com.dhitha.lms.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class BookSeedDataTest {

  private static final Pattern BOOK_ISBN_PATTERN =
      Pattern.compile("insert into book\\([^\\n]+ values \\(\\d+,'(\\d{13})'");

  @Test
  void seedBookIsbnsAreValidIsbn13() throws IOException, URISyntaxException {
    String dataSql =
        Files.readString(
            Paths.get(
                BookSeedDataTest.class.getClassLoader().getResource("data.sql").toURI()));

    Matcher matcher = BOOK_ISBN_PATTERN.matcher(dataSql);
    List<String> isbns =
        matcher.results().map(result -> result.group(1)).collect(Collectors.toList());

    assertEquals(111, isbns.size());
    assertTrue(isbns.stream().allMatch(BookSeedDataTest::hasValidIsbn13CheckDigit));
  }

  @Test
  void seedDataSqlDoesNotStartWithUtf8Bom() throws IOException, URISyntaxException {
    byte[] bytes =
        Files.readAllBytes(
            Paths.get(
                BookSeedDataTest.class.getClassLoader().getResource("data.sql").toURI()));

    boolean startsWithBom =
        bytes.length >= 3
            && (bytes[0] & 0xFF) == 0xEF
            && (bytes[1] & 0xFF) == 0xBB
            && (bytes[2] & 0xFF) == 0xBF;
    assertTrue(!startsWithBom);
  }

  private static boolean hasValidIsbn13CheckDigit(String isbn) {
    int sum = 0;
    for (int i = 0; i < 12; i++) {
      int digit = Character.digit(isbn.charAt(i), 10);
      sum += i % 2 == 0 ? digit : digit * 3;
    }
    int checkDigit = (10 - (sum % 10)) % 10;
    return Character.digit(isbn.charAt(12), 10) == checkDigit;
  }
}
