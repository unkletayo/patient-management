package com.pm.billingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm.billingservice.exception.BillingAccountNotFoundException;
import com.pm.billingservice.exception.InvalidBillingRequestException;
import com.pm.billingservice.model.BillingAccount;
import com.pm.billingservice.model.BillingAccountStatus;
import com.pm.billingservice.repository.BillingAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

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
}
