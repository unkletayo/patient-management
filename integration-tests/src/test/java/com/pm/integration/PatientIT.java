package com.pm.integration;

import static com.pm.integration.support.Envelope.assertError;
import static com.pm.integration.support.Envelope.assertSuccess;
import static com.pm.integration.support.Envelope.assertValidationError;
import static com.pm.integration.support.GatewayClient.anonymous;
import static com.pm.integration.support.GatewayClient.authenticated;
import static com.pm.integration.support.GatewayClient.withToken;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.pm.integration.support.BaseIT;
import com.pm.integration.support.GatewayClient;
import com.pm.integration.support.TestData;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Patient API via gateway")
class PatientIT extends BaseIT {

  /** Ids of patients created by this class that still need deleting. */
  private static final List<String> CREATED_IDS = new CopyOnWriteArrayList<>();

  @Nested
  @DisplayName("authorization")
  class Authorization {

    @Test
    void listReturns401EnvelopeWithoutToken() {
      assertError(anonymous().get("/api/patients").then(), 401);
    }

    @Test
    void listReturns401EnvelopeWithBadToken() {
      assertError(withToken("garbage").get("/api/patients").then(), 401);
    }
  }

  @Nested
  @DisplayName("GET /api/patients pagination")
  class Pagination {

    @Test
    void returnsPageShape() {
      assertSuccess(
              authenticated()
                  .queryParam("page", 0)
                  .queryParam("size", 2)
                  .queryParam("sort", "name,asc")
                  .get("/api/patients")
                  .then())
          .body("responseBody.content", notNullValue())
          .body("responseBody.pageNumber", equalTo(0))
          .body("responseBody.pageSize", equalTo(2))
          .body("responseBody.totalElements", notNullValue())
          .body("responseBody.totalPages", notNullValue())
          .body("responseBody.first", equalTo(true))
          .body("responseBody.last", notNullValue())
          .body("responseBody.empty", notNullValue())
          .body("responseBody.content.size()", lessThanOrEqualTo(2));
    }

    @Test
    void clampsSizeToOneHundred() {
      assertSuccess(authenticated().queryParam("size", 1000).get("/api/patients").then())
          .body("responseBody.pageSize", equalTo(100))
          .body("responseBody.content.size()", lessThanOrEqualTo(100));
    }

    @Test
    void rejectsUnknownSortField() {
      assertError(
          authenticated().queryParam("sort", "doesNotExist,asc").get("/api/patients").then(), 400);
    }

    @Test
    void sortDirectionControlsOrdering() {
      Map<String, Object> first = TestData.patient();
      first.put("name", "AAA " + TestData.uniqueSuffix());
      Map<String, Object> last = TestData.patient();
      last.put("name", "ZZZ " + TestData.uniqueSuffix());
      createPatient(first);
      createPatient(last);

      assertNotEquals(
          firstNameSortedBy("name,asc"),
          firstNameSortedBy("name,desc"),
          "ascending and descending pages must start with different patients");
    }

    private String firstNameSortedBy(String sort) {
      return assertSuccess(
              authenticated()
                  .queryParam("size", 1)
                  .queryParam("sort", sort)
                  .get("/api/patients")
                  .then())
          .extract()
          .jsonPath()
          .getString("responseBody.content[0].name");
    }
  }

  @Nested
  @DisplayName("patient lifecycle")
  class Lifecycle {

    @Test
    void createsReadsUpdatesAndDeletes() {
      Map<String, Object> payload = TestData.patient();
      String id = createPatient(payload);

      Map<String, Object> update = TestData.patientUpdate((String) payload.get("email"));
      assertSuccess(authenticated().body(update).put("/api/patients/{id}", id).then())
          .body("responseBody.id", equalTo(id))
          .body("responseBody.name", equalTo(update.get("name")))
          .body("responseBody.address", equalTo(update.get("address")))
          .body("responseBody.dateOfBirth", equalTo(update.get("dateOfBirth")));

      assertSuccess(authenticated().delete("/api/patients/{id}", id).then());
      CREATED_IDS.remove(id);

      assertError(authenticated().delete("/api/patients/{id}", id).then(), 404);
    }

    @Test
    void createReturnsAllPatientFields() {
      Map<String, Object> payload = TestData.patient();
      ValidatableResponse response =
          assertSuccess(authenticated().body(payload).post("/api/patients").then());
      CREATED_IDS.add(response.extract().jsonPath().getString("responseBody.id"));

      response
          .body("responseBody.id", notNullValue())
          .body("responseBody.name", equalTo(payload.get("name")))
          .body("responseBody.email", equalTo(payload.get("email")))
          .body("responseBody.address", equalTo(payload.get("address")))
          .body("responseBody.dateOfBirth", equalTo(payload.get("dateOfBirth")))
          .body("responseBody.registeredDate", equalTo(payload.get("registeredDate")));
    }

    @Test
    void updateAllowsKeepingOwnEmail() {
      Map<String, Object> payload = TestData.patient();
      String id = createPatient(payload);

      assertSuccess(
          authenticated()
              .body(TestData.patientUpdate((String) payload.get("email")))
              .put("/api/patients/{id}", id)
              .then());
    }
  }

  @Nested
  @DisplayName("conflicts")
  class Conflicts {

    @Test
    void createWithDuplicateEmailReturns409() {
      Map<String, Object> payload = TestData.patient();
      createPatient(payload);

      assertError(
          authenticated().body(TestData.patient((String) payload.get("email"))).post("/api/patients").then(),
          409);
    }

    @Test
    void updateToAnotherPatientsEmailReturns409() {
      Map<String, Object> other = TestData.patient();
      createPatient(other);
      String id = createPatient(TestData.patient());

      assertError(
          authenticated()
              .body(TestData.patientUpdate((String) other.get("email")))
              .put("/api/patients/{id}", id)
              .then(),
          409);
    }
  }

  @Nested
  @DisplayName("not found and malformed ids")
  class NotFound {

    private final String unknownId = UUID.randomUUID().toString();

    @Test
    void updateUnknownIdReturns404() {
      assertError(
          authenticated()
              .body(TestData.patientUpdate(TestData.uniqueEmail()))
              .put("/api/patients/{id}", unknownId)
              .then(),
          404);
    }

    @Test
    void deleteUnknownIdReturns404() {
      assertError(authenticated().delete("/api/patients/{id}", unknownId).then(), 404);
    }

    @Test
    void updateMalformedIdReturns400() {
      assertError(
          authenticated()
              .body(TestData.patientUpdate(TestData.uniqueEmail()))
              .put("/api/patients/{id}", "not-a-uuid")
              .then(),
          400);
    }

    @Test
    void deleteMalformedIdReturns400() {
      assertError(authenticated().delete("/api/patients/{id}", "not-a-uuid").then(), 400);
    }
  }

  @Nested
  @DisplayName("validation")
  class Validation {

    @Test
    void createWithEmptyBodyFieldsReturns400FieldMap() {
      assertValidationError(
          authenticated().body(Map.of()).post("/api/patients").then(),
          "name",
          "email",
          "address",
          "dateOfBirth",
          "registeredDate");
    }

    @Test
    void createWithInvalidEmailReturns400() {
      assertValidationError(
          authenticated().body(TestData.patient("not-an-email")).post("/api/patients").then(),
          "email");
    }

    @Test
    void createWithTooLongNameReturns400() {
      Map<String, Object> payload = TestData.patient();
      payload.put("name", "x".repeat(101));
      assertValidationError(authenticated().body(payload).post("/api/patients").then(), "name");
    }

    @Test
    void createWithoutRegisteredDateReturns400() {
      Map<String, Object> payload = TestData.patient();
      payload.remove("registeredDate");
      assertValidationError(
          authenticated().body(payload).post("/api/patients").then(), "registeredDate");
    }

    @Test
    void updateWithInvalidBodyReturns400() {
      String id = createPatient(TestData.patient());
      Map<String, Object> update = TestData.patientUpdate("not-an-email");
      update.put("name", "");
      assertValidationError(
          authenticated().body(update).put("/api/patients/{id}", id).then(), "email", "name");
    }

    @Test
    void createWithoutTokenReturns401() {
      assertError(anonymous().body(TestData.patient()).post("/api/patients").then(), 401);
    }
  }

  /** Creates a patient through the API and registers it for cleanup; returns its id. */
  private static String createPatient(Map<String, Object> payload) {
    String id =
        assertSuccess(authenticated().body(payload).post("/api/patients").then())
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
      // best effort: the test itself may already have deleted the patient
      authenticated().delete("/api/patients/{id}", id).then().extract().statusCode();
    }
    CREATED_IDS.clear();
  }
}
