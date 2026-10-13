package com.pm.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "auth.service.timeout=2s")
@AutoConfigureWebTestClient(timeout = "15s")
class JwtValidationFilterTest {

  private static final String VALID_ENVELOPE = """
      {"requestSuccessful":true,"responseMessage":"success","responseCode":"0",
       "responseBody":{"email":"jane@example.com","role":"ADMIN"}}""";
  private static final String UNAUTHORIZED_ENVELOPE = """
      {"requestSuccessful":false,"responseMessage":"Unauthorized",
       "responseCode":"401","responseBody":null}""";

  private static MockWebServer authServer;
  private static MockWebServer patientServer;

  @Autowired
  private WebTestClient client;

  @BeforeAll
  static void startServers() throws IOException {
    authServer = new MockWebServer();
    authServer.start();
    patientServer = new MockWebServer();
    patientServer.start();
  }

  @BeforeEach
  void drainRecordedRequests() throws InterruptedException {
    while (authServer.takeRequest(50, TimeUnit.MILLISECONDS) != null) {
      // discard requests recorded by earlier tests
    }
    while (patientServer.takeRequest(50, TimeUnit.MILLISECONDS) != null) {
      // discard requests recorded by earlier tests
    }
  }

  @AfterAll
  static void stopServers() throws IOException {
    authServer.close();
    patientServer.close();
  }

  @DynamicPropertySource
  static void upstreams(DynamicPropertyRegistry registry) throws IOException {
    if (authServer == null) {
      startServers();
    }
    registry.add("auth.service.url", () -> authServer.url("/").toString());
    registry.add("patient.service.url", () -> patientServer.url("/").toString());
  }

  private static MockResponse json(int code, String body) {
    return new MockResponse.Builder()
        .code(code)
        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(body)
        .build();
  }

  @Test
  void missingAuthorizationHeaderReturns401Envelope() {
    client.get().uri("/api/patients")
        .exchange()
        .expectStatus().isUnauthorized()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.requestSuccessful").isEqualTo(false)
        .jsonPath("$.responseCode").isEqualTo("401");
  }

  @Test
  void malformedAuthorizationHeaderReturns401Envelope() {
    client.get().uri("/api/patients")
        .header(HttpHeaders.AUTHORIZATION, "Basic abc")
        .exchange()
        .expectStatus().isUnauthorized()
        .expectBody()
        .jsonPath("$.requestSuccessful").isEqualTo(false)
        .jsonPath("$.responseCode").isEqualTo("401");
  }

  @Test
  void validTokenIsForwardedWithValidatedIdentityAndSpoofedHeadersStripped()
      throws InterruptedException {
    authServer.enqueue(json(200, VALID_ENVELOPE));
    patientServer.enqueue(json(200, "[]"));

    client.get().uri("/api/patients")
        .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
        .header("X-User-Email", "attacker@evil.com")
        .header("X-User-Role", "SUPERUSER")
        .exchange()
        .expectStatus().isOk();

    RecordedRequest validation = authServer.takeRequest(2, TimeUnit.SECONDS);
    assertThat(validation).isNotNull();
    assertThat(validation.getUrl().encodedPath()).isEqualTo("/validate");
    assertThat(validation.getHeaders().get(HttpHeaders.AUTHORIZATION))
        .isEqualTo("Bearer good-token");

    RecordedRequest forwarded = patientServer.takeRequest(2, TimeUnit.SECONDS);
    assertThat(forwarded).isNotNull();
    assertThat(forwarded.getUrl().encodedPath()).isEqualTo("/patients");
    assertThat(forwarded.getHeaders().values("X-User-Email"))
        .containsExactly("jane@example.com");
    assertThat(forwarded.getHeaders().values("X-User-Role"))
        .containsExactly("ADMIN");
  }

  @Test
  void spoofedIdentityHeadersAreStrippedOnPublicAuthRoute() throws InterruptedException {
    authServer.enqueue(json(200, VALID_ENVELOPE));

    client.post().uri("/auth/login")
        .header("X-User-Email", "attacker@evil.com")
        .header("X-User-Role", "SUPERUSER")
        .exchange()
        .expectStatus().isOk();

    RecordedRequest forwarded = authServer.takeRequest(2, TimeUnit.SECONDS);
    assertThat(forwarded).isNotNull();
    assertThat(forwarded.getUrl().encodedPath()).isEqualTo("/login");
    assertThat(forwarded.getHeaders().values("X-User-Email")).isEmpty();
    assertThat(forwarded.getHeaders().values("X-User-Role")).isEmpty();
  }

  @Test
  void authServiceRejectionReturns401Envelope() {
    authServer.enqueue(json(401, UNAUTHORIZED_ENVELOPE));

    client.get().uri("/api/patients")
        .header(HttpHeaders.AUTHORIZATION, "Bearer bad-token")
        .exchange()
        .expectStatus().isUnauthorized()
        .expectBody()
        .jsonPath("$.requestSuccessful").isEqualTo(false)
        .jsonPath("$.responseCode").isEqualTo("401");
  }

  @Test
  void authServiceServerErrorReturns503Envelope() {
    authServer.enqueue(json(500, "{}"));

    client.get().uri("/api/patients")
        .header(HttpHeaders.AUTHORIZATION, "Bearer any-token")
        .exchange()
        .expectStatus().isEqualTo(503)
        .expectBody()
        .jsonPath("$.requestSuccessful").isEqualTo(false)
        .jsonPath("$.responseCode").isEqualTo("503");
  }

  @Test
  void slowAuthServiceReturns503Envelope() {
    authServer.enqueue(new MockResponse.Builder()
        .code(200)
        .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .body(VALID_ENVELOPE)
        .headersDelay(4, TimeUnit.SECONDS)
        .build());

    client.get().uri("/api/patients")
        .header(HttpHeaders.AUTHORIZATION, "Bearer slow-token")
        .exchange()
        .expectStatus().isEqualTo(503)
        .expectBody()
        .jsonPath("$.responseCode").isEqualTo("503");
  }

  @Test
  void unknownRouteReturns404Envelope() {
    client.get().uri("/no/such/route")
        .exchange()
        .expectStatus().isNotFound()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.requestSuccessful").isEqualTo(false)
        .jsonPath("$.responseCode").isEqualTo("404");
  }
}
