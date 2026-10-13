package com.pm.integration;

import static com.pm.integration.support.Envelope.assertError;
import static com.pm.integration.support.Envelope.assertSuccess;
import static com.pm.integration.support.Envelope.assertValidationError;
import static com.pm.integration.support.GatewayClient.SEED_EMAIL;
import static com.pm.integration.support.GatewayClient.SEED_PASSWORD;
import static com.pm.integration.support.GatewayClient.anonymous;
import static com.pm.integration.support.GatewayClient.token;
import static com.pm.integration.support.GatewayClient.withToken;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import com.pm.integration.support.BaseIT;
import com.pm.integration.support.GatewayClient.LoginBody;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Auth API via gateway")
class AuthIT extends BaseIT {

  @Nested
  @DisplayName("POST /auth/login")
  class Login {

    @Test
    void returnsTokenForValidCredentials() {
      assertSuccess(
              anonymous().body(new LoginBody(SEED_EMAIL, SEED_PASSWORD)).post("/auth/login").then())
          .body("responseBody.token", not(emptyOrNullString()));
    }

    @Test
    void returns401EnvelopeForUnknownUser() {
      assertError(
          anonymous()
              .body(new LoginBody("invalid_user@test.com", "wrongpassword"))
              .post("/auth/login")
              .then(),
          401);
    }

    @Test
    void returns401EnvelopeForWrongPassword() {
      assertError(
          anonymous()
              .body(new LoginBody(SEED_EMAIL, "definitely-wrong-password"))
              .post("/auth/login")
              .then(),
          401);
    }

    @Test
    void returns400WithFieldMapForInvalidEmail() {
      assertValidationError(
          anonymous().body(new LoginBody("not-an-email", SEED_PASSWORD)).post("/auth/login").then(),
          "email");
    }

    @Test
    void returns400WithFieldMapForBlankPassword() {
      assertValidationError(
          anonymous().body(new LoginBody(SEED_EMAIL, "")).post("/auth/login").then(), "password");
    }

    @Test
    void returns400WithFieldMapForShortPassword() {
      assertValidationError(
          anonymous().body(new LoginBody(SEED_EMAIL, "short")).post("/auth/login").then(),
          "password");
    }

    @Test
    void returns400ListingEveryInvalidField() {
      assertValidationError(
          anonymous().body(new LoginBody("", "")).post("/auth/login").then(), "email", "password");
    }
  }

  @Nested
  @DisplayName("GET /auth/validate")
  class Validate {

    @Test
    void returnsIdentityForValidToken() {
      assertSuccess(withToken(token()).get("/auth/validate").then())
          .body("responseBody.email", equalTo(SEED_EMAIL))
          .body("responseBody.role", notNullValue());
    }

    @Test
    void returns401EnvelopeForInvalidToken() {
      assertError(withToken("not.a.valid-token").get("/auth/validate").then(), 401);
    }

    @Test
    void returns401EnvelopeForTamperedToken() {
      assertError(withToken(token() + "tampered").get("/auth/validate").then(), 401);
    }

    @Test
    void returns401EnvelopeWhenAuthorizationHeaderIsMissing() {
      assertError(anonymous().get("/auth/validate").then(), 401);
    }

    @Test
    void returns401EnvelopeForNonBearerScheme() {
      assertError(
          anonymous().header("Authorization", "Basic dGVzdDp0ZXN0").get("/auth/validate").then(),
          401);
    }
  }
}
