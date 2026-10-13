package com.pm.billingservice.model;

import java.util.Set;

public enum BillingAccountStatus {
  ACTIVE,
  INACTIVE,
  SUSPENDED,
  CLOSED;

  /**
   * Whether an account may move from this status to the target. Staying in the same status is
   * always allowed (no-op). CLOSED is terminal. INACTIVE is a legacy value with no transitions.
   */
  public boolean canTransitionTo(BillingAccountStatus target) {
    return this == target || allowedTargets().contains(target);
  }

  private Set<BillingAccountStatus> allowedTargets() {
    return switch (this) {
      case ACTIVE -> Set.of(SUSPENDED, CLOSED);
      case SUSPENDED -> Set.of(ACTIVE, CLOSED);
      case INACTIVE, CLOSED -> Set.of();
    };
  }
}
