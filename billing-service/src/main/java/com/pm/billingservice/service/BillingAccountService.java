package com.pm.billingservice.service;

import com.pm.billingservice.exception.BillingAccountNotFoundException;
import com.pm.billingservice.exception.InvalidBillingRequestException;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.model.BillingAccountStatus;
import com.pm.billingservice.repository.BillingAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingAccountService {

  private static final Logger log = LoggerFactory.getLogger(BillingAccountService.class);

  private final BillingAccountRepository repository;

  public BillingAccountService(BillingAccountRepository repository) {
    this.repository = repository;
  }

  /**
   * Creates a billing account for the patient, or returns the existing one (idempotent).
   */
  public BillingAccount createAccount(String patientId, String name, String email) {
    requireText(patientId, "patientId");
    requireText(name, "name");
    requireText(email, "email");

    String normalizedPatientId = patientId.strip();
    return repository.findByPatientId(normalizedPatientId).orElseGet(() -> {
      try {
        BillingAccount created = repository.saveAndFlush(
            new BillingAccount(normalizedPatientId, name.strip(), email.strip()));
        log.info("Created billing account {} for patient {}", created.getId(), normalizedPatientId);
        return created;
      } catch (DataIntegrityViolationException e) {
        // concurrent creation for the same patient: return the winner
        return repository.findByPatientId(normalizedPatientId).orElseThrow(() -> e);
      }
    });
  }

  @Transactional(readOnly = true)
  public Page<BillingAccount> list(int page, int size) {
    return repository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
  }

  @Transactional(readOnly = true)
  public BillingAccount getByPatientId(String patientId) {
    return repository.findByPatientId(patientId)
        .orElseThrow(() -> new BillingAccountNotFoundException(patientId));
  }

  /**
   * Changes the account status. Requesting the current status is a no-op.
   */
  @Transactional
  public BillingAccount updateStatus(String patientId, BillingAccountStatus status) {
    BillingAccount account = repository.findByPatientId(patientId)
        .orElseThrow(() -> new BillingAccountNotFoundException(patientId));
    BillingAccountStatus previous = account.getStatus();
    account.changeStatus(status);
    if (previous != account.getStatus()) {
      log.info("Billing account {} status changed from {} to {}",
          account.getId(), previous, account.getStatus());
    }
    return account;
  }

  private static void requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new InvalidBillingRequestException(field + " must not be blank");
    }
  }
}
