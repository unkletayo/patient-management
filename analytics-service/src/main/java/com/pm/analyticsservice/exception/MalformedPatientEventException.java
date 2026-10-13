package com.pm.analyticsservice.exception;

/**
 * Raised when a Kafka record cannot be parsed into a {@code PatientEvent}.
 * Retrying such a record can never succeed, so it is routed straight to the dead-letter topic.
 */
public class MalformedPatientEventException extends RuntimeException {

  public MalformedPatientEventException(String message, Throwable cause) {
    super(message, cause);
  }
}
