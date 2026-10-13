package com.pm.integration.support;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.response.ValidatableResponse;

/**
 * Assertions for the Monnify-style response envelope:
 * {@code {requestSuccessful, responseMessage, responseCode, responseBody}}.
 */
public final class Envelope {

  private Envelope() {}

  /** HTTP 200 with a successful envelope; returns the response for further body assertions. */
  public static ValidatableResponse assertSuccess(ValidatableResponse response) {
    return response
        .statusCode(200)
        .body("requestSuccessful", equalTo(true))
        .body("responseCode", anyOf(equalTo("0"), equalTo("success")))
        .body("responseMessage", notNullValue());
  }

  /** Error envelope whose HTTP status matches and whose responseCode is that status as a string. */
  public static ValidatableResponse assertError(ValidatableResponse response, int status) {
    return response
        .statusCode(status)
        .body("requestSuccessful", equalTo(false))
        .body("responseCode", equalTo(String.valueOf(status)))
        .body("responseMessage", notNullValue());
  }

  /** 400 error envelope whose responseBody is a field-to-message map containing the fields. */
  public static ValidatableResponse assertValidationError(
      ValidatableResponse response, String... fields) {
    ValidatableResponse validated = assertError(response, 400).body("responseBody", notNullValue());
    for (String field : fields) {
      validated.body("responseBody." + field, notNullValue());
    }
    return validated;
  }
}
