package com.pm.billingservice.dto;

public record ApiResponse<T>(
    boolean requestSuccessful,
    String responseMessage,
    String responseCode,
    T responseBody) {

  public static <T> ApiResponse<T> success(T body) {
    return new ApiResponse<>(true, "success", "0", body);
  }

  public static <T> ApiResponse<T> error(String code, String message) {
    return new ApiResponse<>(false, message, code, null);
  }

  public static <T> ApiResponse<T> error(String code, String message, T body) {
    return new ApiResponse<>(false, message, code, body);
  }
}
