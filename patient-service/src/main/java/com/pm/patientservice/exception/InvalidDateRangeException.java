package com.pm.patientservice.exception;

public class InvalidDateRangeException extends RuntimeException {

  public InvalidDateRangeException(String message) {
    super(message);
  }
}
