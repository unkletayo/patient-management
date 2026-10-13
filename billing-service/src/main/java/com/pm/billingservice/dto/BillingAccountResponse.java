package com.pm.billingservice.dto;

import com.pm.billingservice.model.BillingAccount;
import java.time.Instant;

public record BillingAccountResponse(
    String id,
    String patientId,
    String name,
    String email,
    String status,
    Instant createdAt) {

  public static BillingAccountResponse from(BillingAccount account) {
    return new BillingAccountResponse(
        account.getId().toString(),
        account.getPatientId(),
        account.getName(),
        account.getEmail(),
        account.getStatus().name(),
        account.getCreatedAt());
  }
}
