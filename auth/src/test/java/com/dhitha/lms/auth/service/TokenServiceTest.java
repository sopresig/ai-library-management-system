package com.dhitha.lms.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dhitha.lms.auth.TestUtils;
import com.dhitha.lms.auth.dto.UserDTO;
import com.dhitha.lms.auth.error.GenericException;
import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

/**
 * Unit tests for {@link TokenService}
 *
 * @author Dhiraj
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

  @Spy private ResourceLoader resourceLoader = new DefaultResourceLoader();

  private TokenService subject;

  @BeforeEach
  void init() {
    subject = new TokenServiceImpl(resourceLoader);
  }

  @Test
  @DisplayName("generateIdToken: valid input, expected success")
  void testGenerateIdTokenSuccess() throws Exception {
    UserDTO userDTO = TestUtils.createMockUser();
    String result = subject.generateIdToken(userDTO);
    assertEquals(3, result.split("\\.").length);
  }

  @Test
  @DisplayName("generateIdToken: missing configured PEM keys, expected runtime key success")
  void testGenerateIdTokenWithRuntimeKey() throws Exception {
    UserDTO userDTO = TestUtils.createMockUser();
    String mockToken = subject.generateIdToken(userDTO);

    JWTClaimsSet result = subject.verifyToken(mockToken);
    assertEquals("1", result.getSubject());
  }

  @Test
  @DisplayName("verifyToken: valid token, expected success")
  void testVerifyTokenSuccess() throws Exception {
    // Not really a UNIT test??!, can't figure out proper way :(
    UserDTO userDTO = TestUtils.createMockUser();
    String mockToken = subject.generateIdToken(userDTO);

    JWTClaimsSet result = subject.verifyToken(mockToken);
    assertEquals("http://localhost:8081", result.getIssuer());
    assertEquals("1", result.getSubject());
    assertEquals("name", result.getClaim("name"));
    assertEquals("ADMIN,USER", result.getClaim("roles"));
  }

  @Test
  @DisplayName("verifyToken: malformed token, expected GenericException")
  void testVerifyMalformedToken() {
    assertThrows(
        GenericException.class,
        () -> subject.verifyToken("token"));
  }

  @Test
  @DisplayName("verifyToken: expired token, expected GenericException")
  void testVerifyTokenExpiredToken() {
    assertThrows(
        GenericException.class,
        () -> {
          Date issuedAt = Date.from(Instant.now().minus(31, ChronoUnit.MINUTES));
          String mockToken = subject.generateIdToken(TestUtils.createMockUser(), issuedAt);
          subject.verifyToken(mockToken);
        });
  }
}
