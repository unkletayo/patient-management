package com.pm.integration;

import static com.pm.integration.support.Envelope.assertError;
import static com.pm.integration.support.Envelope.assertSuccess;
import static com.pm.integration.support.GatewayClient.anonymous;
import static com.pm.integration.support.GatewayClient.authenticated;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import com.pm.integration.support.BaseIT;
import com.pm.integration.support.GatewayClient;
import com.pm.integration.support.TestData;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Billing API via gateway")
class BillingIT extends BaseIT {

  private static final String ACCOUNTS = "/api/billing-accounts";

  /** Ids of patients created by this class that still need deleting. */
  private static final List<String> CREATED_IDS = new CopyOnWriteArrayList<>();

  @Nested
  @DisplayName("read")
  class Read {

    @Test
    void listWithoutTokenReturns401() {
      assertError(anonymous().get(ACCOUNTS).then(), 401);
    }

    @Test
    void listIsPaginated() {
      assertSuccess(authenticated().queryParam("size", 2).get(ACCOUNTS).then())
          .body("responseBody.pageSize", equalTo(2))
          .body("responseBody.content.size()", lessThanOrEqualTo(2));
    }

    @Test
    void creatingAPatientCreatesABillingAccount() {
      String patientId = createPatient();

      assertSuccess(authenticated().get(ACCOUNTS + "/{patientId}", patientId).then())
          .body("responseBody.patientId", equalTo(patientId))
          .body("responseBody.status", equalTo("ACTIVE"));
    }

    @Test
    void unknownPatientReturns404() {
      assertError(
          authenticated().get(ACCOUNTS + "/{patientId}", UUID.randomUUID().toString()).then(), 404);
    }
  }

  @Nested
  @DisplayName("PATCH /status")
  class StatusUpdate {

    @Test
    void walksThroughSuspendReactivateAndClose() {
      String patientId = createPatient();

      assertStatusAfter(patientId, "SUSPENDED");
      assertStatusAfter(patientId, "ACTIVE");
      assertStatusAfter(patientId, "CLOSED");
    }

    @Test
    void sameStatusIsANoOp() {
      String patientId = createPatient();

      assertStatusAfter(patientId, "ACTIVE");
    }

    @Test
    void closedIsTerminal() {
      String patientId = createPatient();
      assertStatusAfter(patientId, "CLOSED");

      assertError(
          authenticated()
              .body(Map.of("status", "ACTIVE"))
              .patch(ACCOUNTS + "/{patientId}/status", patientId)
              .then(),
          409);
    }

    @Test
    void unknownStatusReturns400() {
      String patientId = createPatient();

      assertError(
          authenticated()
              .body(Map.of("status", "BOGUS"))
              .patch(ACCOUNTS + "/{patientId}/status", patientId)
              .then(),
          400);
    }

    @Test
    void missingStatusReturns400() {
      String patientId = createPatient();

      assertError(
          authenticated()
              .body(Map.of())
              .patch(ACCOUNTS + "/{patientId}/status", patientId)
              .then(),
          400);
    }

    @Test
    void unknownPatientReturns404() {
      assertError(
          authenticated()
              .body(Map.of("status", "SUSPENDED"))
              .patch(ACCOUNTS + "/{patientId}/status", UUID.randomUUID().toString())
              .then(),
          404);
    }

    @Test
    void withoutTokenReturns401() {
      assertError(
          anonymous()
              .body(Map.of("status", "SUSPENDED"))
              .patch(ACCOUNTS + "/{patientId}/status", UUID.randomUUID().toString())
              .then(),
          401);
    }

    private void assertStatusAfter(String patientId, String status) {
      assertSuccess(
              authenticated()
                  .body(Map.of("status", status))
                  .patch(ACCOUNTS + "/{patientId}/status", patientId)
                  .then())
          .body("responseBody.patientId", equalTo(patientId))
          .body("responseBody.status", equalTo(status));
    }
  }

  /** Creates a patient (and so its billing account) through the API; returns the patient id. */
  private static String createPatient() {
    String id =
        assertSuccess(authenticated().body(TestData.patient()).post("/api/patients").then())
            .extract()
            .jsonPath()
            .getString("responseBody.id");
    CREATED_IDS.add(id);
    return id;
  }

  @AfterAll
  static void deleteCreatedPatients() {
    if (!GatewayClient.isReachable()) {
      return;
    }
    for (String id : CREATED_IDS) {
      authenticated().delete("/api/patients/{id}", id).then().extract().statusCode();
    }
    CREATED_IDS.clear();
  }
}
