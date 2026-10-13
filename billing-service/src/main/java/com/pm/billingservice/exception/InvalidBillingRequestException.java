package com.pm.billingservice.exception;

public class InvalidBillingRequestException extends RuntimeException {

  public InvalidBillingRequestException(String message) {
    super(message);
  }
}
