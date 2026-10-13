package com.pm.apigateway.filter;

import java.io.IOException;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AuthServiceDownTest {

  @Autowired
  private WebTestClient client;

  @DynamicPropertySource
  static void closedAuthPort(DynamicPropertyRegistry registry) throws IOException {
    // Bind then immediately release a port so nothing is listening on it.
    MockWebServer server = new MockWebServer();
    server.start();
    String url = server.url("/").toString();
    server.close();
    registry.add("auth.service.url", () -> url);
  }

  @Test
  void unreachableAuthServiceReturns503Envelope() {
    client.get().uri("/api/patients")
        .header(HttpHeaders.AUTHORIZATION, "Bearer any-token")
        .exchange()
        .expectStatus().isEqualTo(503)
        .expectBody()
        .jsonPath("$.requestSuccessful").isEqualTo(false)
        .jsonPath("$.responseCode").isEqualTo("503");
  }
}
