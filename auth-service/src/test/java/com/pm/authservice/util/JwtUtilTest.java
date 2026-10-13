package com.pm.authservice.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.JwtException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

  private static final String SECRET = Base64.getEncoder()
      .encodeToString("0123456789abcdef0123456789abcdef".getBytes());
  private static final String OTHER_SECRET = Base64.getEncoder()
      .encodeToString("fedcba9876543210fedcba9876543210".getBytes());
  private static final long ONE_HOUR = 3_600_000L;

  @Test
  void validTokenReturnsEmailAndRole() {
    JwtUtil jwtUtil = new JwtUtil(SECRET, ONE_HOUR);

    TokenClaims claims = jwtUtil.validateToken(jwtUtil.generateToken("a@b.com", "ADMIN"));

    assertThat(claims).isEqualTo(new TokenClaims("a@b.com", "ADMIN"));
  }

  @Test
  void expiredTokenIsRejectedWithGenericMessage() {
    JwtUtil jwtUtil = new JwtUtil(SECRET, -1_000L);
    String token = jwtUtil.generateToken("a@b.com", "USER");

    assertThatThrownBy(() -> jwtUtil.validateToken(token))
        .isInstanceOf(JwtException.class)
        .hasMessage("Invalid token");
  }

  @Test
  void tamperedTokenIsRejected() {
    JwtUtil jwtUtil = new JwtUtil(SECRET, ONE_HOUR);
    String token = jwtUtil.generateToken("a@b.com", "USER");
    String[] parts = token.split("\\.");
    String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
        "{\"sub\":\"a@b.com\",\"role\":\"ADMIN\"}".getBytes());
    String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

    assertThatThrownBy(() -> jwtUtil.validateToken(tampered))
        .isInstanceOf(JwtException.class)
        .hasMessage("Invalid token");
  }

  @Test
  void tokenSignedWithAnotherKeyIsRejected() {
    String token = new JwtUtil(OTHER_SECRET, ONE_HOUR).generateToken("a@b.com", "USER");

    assertThatThrownBy(() -> new JwtUtil(SECRET, ONE_HOUR).validateToken(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void garbageTokenIsRejected() {
    JwtUtil jwtUtil = new JwtUtil(SECRET, ONE_HOUR);

    assertThatThrownBy(() -> jwtUtil.validateToken("not-a-jwt"))
        .isInstanceOf(JwtException.class)
        .hasMessage("Invalid token");
  }

  @Test
  void shortSecretFailsFast() {
    String shortSecret = Base64.getEncoder().encodeToString("too-short".getBytes());

    assertThatThrownBy(() -> new JwtUtil(shortSecret, ONE_HOUR))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("too short");
  }

  @Test
  void missingSecretFailsFast() {
    assertThatThrownBy(() -> new JwtUtil("", ONE_HOUR))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JWT_SECRET is not set");
  }

  @Test
  void nonBase64SecretFailsFast() {
    assertThatThrownBy(() -> new JwtUtil("%%% not base64 %%%", ONE_HOUR))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("base64");
  }
}
