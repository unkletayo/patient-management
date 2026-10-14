package com.pm.billingservice.model;

import com.pm.billingservice.exception.InvalidStatusTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "billing_accounts")
public class BillingAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true)
  private String patientId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  @Convert(converter = BillingAccountStatusConverter.class)
  @Column(nullable = false)
  private BillingAccountStatus status = BillingAccountStatus.ACTIVE;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  protected BillingAccount() {
  }

  public BillingAccount(String patientId, String name, String email) {
    this.patientId = patientId;
    this.name = name;
    this.email = email;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  /**
   * Moves the account to the target status. Same-status changes are a no-op.
   *
   * @throws InvalidStatusTransitionException if the transition is not allowed
   */
  public void changeStatus(BillingAccountStatus target) {
    if (!status.canTransitionTo(target)) {
      throw new InvalidStatusTransitionException(status, target);
    }
    status = target;
  }

  public UUID getId() {
    return id;
  }

  public String getPatientId() {
    return patientId;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public BillingAccountStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
