package com.pm.integration.support;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Unique test data so that tests are re-runnable against a persistent database. */
public final class TestData {

  private static final String EMAIL_DOMAIN = "it.example.com";

  private TestData() {}

  public static String uniqueSuffix() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }

  public static String uniqueEmail() {
    return "it-" + uniqueSuffix() + "@" + EMAIL_DOMAIN;
  }

  /** A valid create-patient payload with a unique email. */
  public static Map<String, Object> patient() {
    return patient(uniqueEmail());
  }

  /** A valid create-patient payload with the given email. */
  public static Map<String, Object> patient(String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("name", "IT Patient " + uniqueSuffix());
    body.put("email", email);
    body.put("address", "12 Integration Street");
    body.put("dateOfBirth", "1990-05-17");
    body.put("registeredDate", "2024-01-15");
    return body;
  }

  /** A valid update payload (registeredDate is not part of the update contract). */
  public static Map<String, Object> patientUpdate(String email) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("name", "IT Updated " + uniqueSuffix());
    body.put("email", email);
    body.put("address", "99 Updated Avenue");
    body.put("dateOfBirth", "1985-11-02");
    return body;
  }
}
