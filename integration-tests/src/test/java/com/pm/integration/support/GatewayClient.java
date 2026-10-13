package com.pm.integration.support;

import static io.restassured.RestAssured.given;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.Duration;

/**
 * Entry point for talking to the running stack through the API gateway. Resolves the gateway URL,
 * checks reachability and hands out request specifications (anonymous or authenticated).
 */
public final class GatewayClient {

  public static final String DEFAULT_GATEWAY_URL = "http://localhost:4004";
  public static final String SEED_EMAIL = "testuser@test.com";
  public static final String SEED_PASSWORD = "password123";

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);

  private static volatile String cachedToken;

  private GatewayClient() {}

  /** Gateway base URL: {@code -Dgateway.url}, then {@code GATEWAY_URL} env, then the default. */
  public static String baseUrl() {
    String url = System.getProperty("gateway.url");
    if (isBlank(url)) {
      url = System.getenv("GATEWAY_URL");
    }
    if (isBlank(url)) {
      url = DEFAULT_GATEWAY_URL;
    }
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  /** True when a TCP connection to the gateway host and port can be opened. */
  public static boolean isReachable() {
    URI uri = URI.create(baseUrl());
    String host = uri.getHost();
    int port = uri.getPort() != -1 ? uri.getPort() : "https".equals(uri.getScheme()) ? 443 : 80;
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), (int) CONNECT_TIMEOUT.toMillis());
      return true;
    } catch (IOException | IllegalArgumentException e) {
      return false;
    }
  }

  /** Unauthenticated JSON request specification. */
  public static RequestSpecification anonymous() {
    return given().spec(
        new RequestSpecBuilder()
            .setBaseUri(baseUrl())
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .build());
  }

  /** JSON request specification carrying the given bearer token. */
  public static RequestSpecification withToken(String token) {
    return anonymous().header("Authorization", "Bearer " + token);
  }

  /** JSON request specification authenticated as the seed user. */
  public static RequestSpecification authenticated() {
    return withToken(token());
  }

  /** Logs in as the seed user once and reuses the token for the rest of the run. */
  public static String token() {
    String token = cachedToken;
    if (token == null) {
      synchronized (GatewayClient.class) {
        if (cachedToken == null) {
          cachedToken = login(SEED_EMAIL, SEED_PASSWORD);
        }
        token = cachedToken;
      }
    }
    return token;
  }

  /** Performs a login that is expected to succeed and returns the JWT. */
  public static String login(String email, String password) {
    return anonymous()
        .body(new LoginBody(email, password))
        .post("/auth/login")
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getString("responseBody.token");
  }

  public record LoginBody(String email, String password) {}

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
