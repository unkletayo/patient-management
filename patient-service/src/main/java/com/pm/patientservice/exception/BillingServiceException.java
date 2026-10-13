package com.pm.patientservice.exception;

public class BillingServiceException extends RuntimeException {

  private final boolean unavailable;

  public BillingServiceException(String message, boolean unavailable,
      Throwable cause) {
    super(message, cause);
    this.unavailable = unavailable;
  }

  /**
   * True when billing could not be reached in time (maps to 503), false when
   * it answered with an error (maps to 502).
   */
  public boolean isUnavailable() {
    return unavailable;
  }
}
