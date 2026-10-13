package com.pm.billingservice.controller;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pm.billingservice.exception.BillingAccountNotFoundException;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.service.BillingAccountService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BillingAccountController.class)
class BillingAccountControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private BillingAccountService service;

  private static BillingAccount account() {
    BillingAccount account = new BillingAccount("p-1", "Jane", "jane@example.com");
    ReflectionTestUtils.setField(account, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(account, "createdAt", java.time.Instant.now());
    return account;
  }

  @Test
  void listReturnsPagedEnvelope() throws Exception {
    when(service.list(0, 20))
        .thenReturn(new PageImpl<>(List.of(account()), PageRequest.of(0, 20), 1));

    mockMvc.perform(get("/billing-accounts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestSuccessful").value(true))
        .andExpect(jsonPath("$.responseMessage").value("success"))
        .andExpect(jsonPath("$.responseCode").value("0"))
        .andExpect(jsonPath("$.responseBody.content[0].patientId").value("p-1"))
        .andExpect(jsonPath("$.responseBody.pageSize").value(20))
        .andExpect(jsonPath("$.responseBody.totalElements").value(1))
        .andExpect(jsonPath("$.responseBody.first").value(true));
  }

  @Test
  void listRejectsSizeAboveMax() throws Exception {
    when(service.list(anyInt(), anyInt())).thenThrow(new AssertionError("must not be called"));

    mockMvc.perform(get("/billing-accounts").param("size", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("400"));
  }

  @Test
  void getReturnsAccount() throws Exception {
    when(service.getByPatientId("p-1")).thenReturn(account());

    mockMvc.perform(get("/billing-accounts/p-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.responseBody.status").value("ACTIVE"));
  }

  @Test
  void getUnknownPatientReturns404Envelope() throws Exception {
    when(service.getByPatientId("nope")).thenThrow(new BillingAccountNotFoundException("nope"));

    mockMvc.perform(get("/billing-accounts/nope"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("404"));
  }

  @Test
  void unexpectedErrorReturns500Envelope() throws Exception {
    when(service.getByPatientId("boom")).thenThrow(new IllegalStateException("db password"));

    mockMvc.perform(get("/billing-accounts/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.responseCode").value("500"))
        .andExpect(jsonPath("$.responseMessage").value("Internal server error"));
  }
}
