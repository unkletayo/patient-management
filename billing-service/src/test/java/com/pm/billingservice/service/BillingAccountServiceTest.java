package com.pm.billingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm.billingservice.exception.BillingAccountNotFoundException;
import com.pm.billingservice.exception.InvalidBillingRequestException;
import com.pm.billingservice.exception.InvalidStatusTransitionException;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.model.BillingAccountStatus;
import com.pm.billingservice.repository.BillingAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BillingAccountServiceTest {

  @Mock
  private BillingAccountRepository repository;

  @InjectMocks
  private BillingAccountService service;

  @Test
  void createsAccountWhenPatientHasNone() {
    when(repository.findByPatientId("p-1")).thenReturn(Optional.empty());
    when(repository.saveAndFlush(any(BillingAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    BillingAccount account = service.createAccount(" p-1 ", "Jane", "jane@example.com");

    assertThat(account.getPatientId()).isEqualTo("p-1");
    assertThat(account.getStatus()).isEqualTo(BillingAccountStatus.ACTIVE);
  }

  @Test
  void returnsExistingAccountForSamePatient() {
    BillingAccount existing = new BillingAccount("p-1", "Jane", "jane@example.com");
    when(repository.findByPatientId("p-1")).thenReturn(Optional.of(existing));

    BillingAccount account = service.createAccount("p-1", "Other", "other@example.com");

    assertThat(account).isSameAs(existing);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void returnsWinnerOnConcurrentCreation() {
    BillingAccount winner = new BillingAccount("p-1", "Jane", "jane@example.com");
    when(repository.findByPatientId("p-1"))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(winner));
    when(repository.saveAndFlush(any(BillingAccount.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    assertThat(service.createAccount("p-1", "Jane", "jane@example.com")).isSameAs(winner);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  void rejectsBlankFields(String blank) {
    assertThatThrownBy(() -> service.createAccount(blank, "Jane", "jane@example.com"))
        .isInstanceOf(InvalidBillingRequestException.class);
    assertThatThrownBy(() -> service.createAccount("p-1", blank, "jane@example.com"))
        .isInstanceOf(InvalidBillingRequestException.class);
    assertThatThrownBy(() -> service.createAccount("p-1", "Jane", blank))
        .isInstanceOf(InvalidBillingRequestException.class);
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void throwsWhenAccountNotFound() {
    when(repository.findByPatientId("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getByPatientId("missing"))
        .isInstanceOf(BillingAccountNotFoundException.class);
  }

  private BillingAccount accountWithStatus(BillingAccountStatus status) {
    BillingAccount account = new BillingAccount("p-1", "Jane", "jane@example.com");
    ReflectionTestUtils.setField(account, "status", status);
    when(repository.findByPatientId("p-1")).thenReturn(Optional.of(account));
    return account;
  }

  @ParameterizedTest
  @CsvSource({
      "ACTIVE, SUSPENDED",
      "ACTIVE, CLOSED",
      "SUSPENDED, ACTIVE",
      "SUSPENDED, CLOSED"
  })
  void updatesStatusForAllowedTransitions(BillingAccountStatus from, BillingAccountStatus to) {
    BillingAccount account = accountWithStatus(from);

    BillingAccount updated = service.updateStatus("p-1", to);

    assertThat(updated).isSameAs(account);
    assertThat(updated.getStatus()).isEqualTo(to);
  }

  @ParameterizedTest
  @EnumSource(BillingAccountStatus.class)
  void sameStatusIsNoOp(BillingAccountStatus status) {
    BillingAccount account = accountWithStatus(status);

    assertThat(service.updateStatus("p-1", status).getStatus()).isEqualTo(status);
    assertThat(account.getStatus()).isEqualTo(status);
  }

  @ParameterizedTest
  @EnumSource(value = BillingAccountStatus.class, names = {"ACTIVE", "SUSPENDED", "INACTIVE"})
  void closedIsTerminal(BillingAccountStatus target) {
    BillingAccount account = accountWithStatus(BillingAccountStatus.CLOSED);

    assertThatThrownBy(() -> service.updateStatus("p-1", target))
        .isInstanceOf(InvalidStatusTransitionException.class)
        .hasMessageContaining("CLOSED");
    assertThat(account.getStatus()).isEqualTo(BillingAccountStatus.CLOSED);
  }

  @Test
  void rejectsTransitionToLegacyInactive() {
    BillingAccount account = accountWithStatus(BillingAccountStatus.ACTIVE);

    assertThatThrownBy(() -> service.updateStatus("p-1", BillingAccountStatus.INACTIVE))
        .isInstanceOf(InvalidStatusTransitionException.class);
    assertThat(account.getStatus()).isEqualTo(BillingAccountStatus.ACTIVE);
  }

  @Test
  void updateStatusThrowsWhenAccountNotFound() {
    when(repository.findByPatientId("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateStatus("missing", BillingAccountStatus.CLOSED))
        .isInstanceOf(BillingAccountNotFoundException.class);
  }
}
