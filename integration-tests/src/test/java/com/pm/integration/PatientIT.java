package com.pm.integration;

import static com.pm.integration.support.Envelope.assertError;
import static com.pm.integration.support.Envelope.assertSuccess;
import static com.pm.integration.support.GatewayClient.anonymous;
import static com.pm.integration.support.GatewayClient.authenticated;
import static com.pm.integration.support.GatewayClient.withToken;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.pm.integration.support.BaseIT;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Patient API via gateway")
class PatientIT extends BaseIT {

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
  }
}
