package com.dhitha.lms.auth;

import com.dhitha.lms.auth.dto.UserDTO;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTClaimsSet.Builder;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

/** @author Dhiraj */
public final class TestUtils {

  public static UserDTO createMockUser() {
    return UserDTO.builder()
        .id(1L)
        .name("name")
        .username("username") // pass
        .accountNonExpired(true)
        .accountNonLocked(true)
        .enabled(true)
        .credentialsNonExpired(true)
        .createdAt("2020-12-26T00:00:00")
        .updatedAt("2020-12-26T00:00:00")
        .userRoles(Arrays.asList("ADMIN", "USER"))
        .build();
  }

  public static JWTClaimsSet createMockClaim() {
    return new Builder()
        .subject("1")
        .claim("roles", "ADMIN,USER")
        .claim("name", "name")
        .claim("username", "username")
        .claim("createdAt", "2020-12-26T00:00:00")
        .claim("updatedAt", "2020-12-26T00:00:00")
        .claim("accountNonExpired", true)
        .claim("accountNonLocked", true)
        .claim("enabled", true)
        .claim("credentialsNonExpired", true)
        .issueTime(Date.from(Instant.ofEpochSecond(20)))
        .build();
  }
}
