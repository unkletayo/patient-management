package com.pm.authservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

  private static final String ROLE_CLAIM = "role";
  private static final int MIN_KEY_BYTES = 32; // 256 bits for HS256

  private final SecretKey secretKey;
  private final long expirationMs;

  public JwtUtil(@Value("${jwt.secret:}") String secret,
      @Value("${jwt.expiration-ms:36000000}") long expirationMs) {
    this.secretKey = toSecretKey(secret);
    this.expirationMs = expirationMs;
  }

  public String generateToken(String email, String role) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .subject(email)
        .claim(ROLE_CLAIM, role)
        .issuedAt(new Date(now))
        .expiration(new Date(now + expirationMs))
        .signWith(secretKey)
        .compact();
  }

  /**
   * Verifies the signature and expiry of the token.
   *
   * @throws JwtException with a generic message when the token is not acceptable
   */
  public TokenClaims validateToken(String token) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token)
          .getPayload();

      String email = claims.getSubject();
      String role = claims.get(ROLE_CLAIM, String.class);
      if (email == null || role == null) {
        throw new JwtException("Invalid token");
      }
      return new TokenClaims(email, role);
    } catch (JwtException | IllegalArgumentException e) {
      throw new JwtException("Invalid token");
    }
  }

  private static SecretKey toSecretKey(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException(
          "JWT_SECRET is not set: provide a base64 encoded secret of at least 256 bits "
              + "(jwt.secret / JWT_SECRET)");
    }

    byte[] keyBytes;
    try {
      keyBytes = Decoders.BASE64.decode(secret.trim());
    } catch (RuntimeException e) {
      throw new IllegalStateException("JWT_SECRET must be a valid base64 encoded string");
    }

    if (keyBytes.length < MIN_KEY_BYTES) {
      throw new IllegalStateException(
          "JWT_SECRET is too short: it must decode to at least 256 bits (32 bytes)");
    }
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
