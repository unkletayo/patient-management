package com.pm.billingservice.exception;

import com.pm.billingservice.model.BillingAccountStatus;

public class InvalidStatusTransitionException extends RuntimeException {

  public InvalidStatusTransitionException(BillingAccountStatus from, BillingAccountStatus to) {
    super("Cannot change billing account status from " + from + " to " + to);
  }
}
