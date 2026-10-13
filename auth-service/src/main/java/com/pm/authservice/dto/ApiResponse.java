package com.pm.authservice.dto;

/**
 * Standard response envelope shared by every endpoint of the service.
 *
 * @param requestSuccessful whether the request was processed successfully
 * @param responseMessage   "success" on success, a human readable reason otherwise
 * @param responseCode      "0" on success, the HTTP status code as a string otherwise
 * @param responseBody      payload on success, optional detail (e.g. field errors) on failure
 */
public record ApiResponse<T>(
    boolean requestSuccessful,
    String responseMessage,
    String responseCode,
    T responseBody) {

  private static final String SUCCESS_MESSAGE = "success";
  private static final String SUCCESS_CODE = "0";

  public static <T> ApiResponse<T> success(T body) {
    return new ApiResponse<>(true, SUCCESS_MESSAGE, SUCCESS_CODE, body);
  }

  public static <T> ApiResponse<T> error(String code, String message) {
    return new ApiResponse<>(false, message, code, null);
  }

  public static <T> ApiResponse<T> error(String code, String message, T body) {
    return new ApiResponse<>(false, message, code, body);
  }
}
