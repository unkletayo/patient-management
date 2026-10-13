package com.pm.patientservice.exception;

import com.pm.patientservice.dto.ApiResponse;
import java.time.DateTimeException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(
      GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(
        error -> errors.putIfAbsent(error.getField(),
            error.getDefaultMessage()));
    return respond(HttpStatus.BAD_REQUEST, "Validation failed", errors);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnreadable(
      HttpMessageNotReadableException ex) {
    log.warn("Unreadable request body: {}", ex.getMessage());
    if (hasCause(ex, DateTimeException.class)) {
      return respond(HttpStatus.BAD_REQUEST,
          "Invalid date, expected format yyyy-MM-dd");
    }
    return respond(HttpStatus.BAD_REQUEST, "Malformed JSON request");
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex) {
    return respond(HttpStatus.BAD_REQUEST,
        "Invalid value for parameter '" + ex.getName() + "'");
  }

  @ExceptionHandler(DateTimeException.class)
  public ResponseEntity<ApiResponse<Void>> handleDateTime(
      DateTimeException ex) {
    return respond(HttpStatus.BAD_REQUEST,
        "Invalid date, expected format yyyy-MM-dd");
  }

  @ExceptionHandler(InvalidSortException.class)
  public ResponseEntity<ApiResponse<Void>> handleInvalidSort(
      InvalidSortException ex) {
    return respond(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(InvalidDateRangeException.class)
  public ResponseEntity<ApiResponse<Void>> handleInvalidDateRange(
      InvalidDateRangeException ex) {
    return respond(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(PatientNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotFound(
      PatientNotFoundException ex) {
    log.warn("Patient not found: {}", ex.getMessage());
    return respond(HttpStatus.NOT_FOUND, "Patient not found");
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ApiResponse<Void>> handleEmailExists(
      EmailAlreadyExistsException ex) {
    log.warn("Duplicate email: {}", ex.getMessage());
    return respond(HttpStatus.CONFLICT, "Email address already exists");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
      DataIntegrityViolationException ex) {
    log.warn("Data integrity violation", ex);
    return respond(HttpStatus.CONFLICT,
        "Request conflicts with existing data (e.g. duplicate email)");
  }

  @ExceptionHandler(BillingServiceException.class)
  public ResponseEntity<ApiResponse<Void>> handleBilling(
      BillingServiceException ex) {
    log.error("Billing service failure", ex);
    return ex.isUnavailable()
        ? respond(HttpStatus.SERVICE_UNAVAILABLE,
            "Billing service is unavailable")
        : respond(HttpStatus.BAD_GATEWAY, "Billing service request failed");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleAny(Exception ex) {
    if (ex instanceof ErrorResponse errorResponse) {
      HttpStatusCode status = errorResponse.getStatusCode();
      if (status.is5xxServerError()) {
        log.error("Request failed", ex);
      }
      return respond(status, status.is5xxServerError()
          ? "Internal server error"
          : errorResponse.getBody().getDetail());
    }
    log.error("Unhandled exception", ex);
    return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  private static <T> ResponseEntity<ApiResponse<T>> respond(
      HttpStatusCode status, String message) {
    return respond(status, message, null);
  }

  private static <T> ResponseEntity<ApiResponse<T>> respond(
      HttpStatusCode status, String message, T body) {
    return ResponseEntity.status(status).body(
        ApiResponse.error(String.valueOf(status.value()), message, body));
  }

  private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
    for (Throwable t = ex; t != null; t = t.getCause()) {
      if (type.isInstance(t)) {
        return true;
      }
    }
    return false;
  }
}
