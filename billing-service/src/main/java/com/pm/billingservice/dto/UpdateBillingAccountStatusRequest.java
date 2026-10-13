package com.pm.billingservice.dto;

import com.pm.billingservice.model.BillingAccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateBillingAccountStatusRequest(@NotNull BillingAccountStatus status) {
}
