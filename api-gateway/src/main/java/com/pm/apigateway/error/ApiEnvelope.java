package com.pm.apigateway.error;

/** Monnify-style response envelope shared by all services. */
public record ApiEnvelope<T>(
    boolean requestSuccessful,
    String responseMessage,
    String responseCode,
    T responseBody) {

  public static ApiEnvelope<Void> failure(int status, String message) {
    return new ApiEnvelope<>(false, message, String.valueOf(status), null);
  }
}
