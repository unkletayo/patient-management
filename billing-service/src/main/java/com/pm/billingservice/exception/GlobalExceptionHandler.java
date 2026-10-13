package com.pm.billingservice.exception;

import com.pm.billingservice.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BillingAccountNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotFound(BillingAccountNotFoundException e) {
    return build(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler({
      InvalidBillingRequestException.class,
      ConstraintViolationException.class,
      HandlerMethodValidationException.class,
      MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception e) {
    String message = e instanceof InvalidBillingRequestException ? e.getMessage()
        : "Invalid request parameters";
    return build(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
    if (e instanceof ErrorResponse errorResponse
        && errorResponse.getStatusCode() instanceof HttpStatus status
        && status.is4xxClientError()) {
      return build(status, status.getReasonPhrase());
    }
    log.error("Unhandled exception", e);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  private static ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(ApiResponse.error(String.valueOf(status.value()), message));
  }
}
