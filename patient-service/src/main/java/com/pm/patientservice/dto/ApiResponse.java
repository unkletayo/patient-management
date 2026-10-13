package com.pm.patientservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard response envelope")
public record ApiResponse<T>(
    @Schema(description = "True when the request succeeded")
    boolean requestSuccessful,
    @Schema(description = "Human readable outcome", example = "success")
    String responseMessage,
    @Schema(description = "\"0\" on success, otherwise the HTTP status code", example = "0")
    String responseCode,
    @Schema(description = "Payload, or a field -> message map for validation failures")
    T responseBody) {

  private static final String SUCCESS_CODE = "0";
  private static final String SUCCESS_MESSAGE = "success";

  public static <T> ApiResponse<T> success(T body) {
    return success(SUCCESS_MESSAGE, body);
  }

  public static <T> ApiResponse<T> success(String message, T body) {
    return new ApiResponse<>(true, message, SUCCESS_CODE, body);
  }

  public static <T> ApiResponse<T> error(String code, String message) {
    return error(code, message, null);
  }

  public static <T> ApiResponse<T> error(String code, String message, T body) {
    return new ApiResponse<>(false, message, code, body);
  }
}
