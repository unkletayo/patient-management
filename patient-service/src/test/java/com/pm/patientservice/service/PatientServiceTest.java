package com.pm.patientservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pm.patientservice.dto.PageResponse;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.BillingServiceException;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

  @Mock
  PatientRepository patientRepository;
  @Mock
  BillingServiceGrpcClient billingClient;
  @Mock
  KafkaProducer kafkaProducer;
  @InjectMocks
  PatientService service;

  private static final LocalDate DOB = LocalDate.of(1985, 6, 15);

  private static PatientRequestDTO request(String email) {
    return new PatientRequestDTO("John Doe", email, "1 Main St", DOB,
        LocalDate.of(2024, 1, 10));
  }

  private static Patient patient(UUID id, String email) {
    Patient p = new Patient();
    p.setId(id);
    p.setName("John Doe");
    p.setEmail(email);
    p.setAddress("1 Main St");
    p.setDateOfBirth(DOB);
    p.setRegisteredDate(LocalDate.of(2024, 1, 10));
    return p;
  }

  @SuppressWarnings("unchecked")
  private static Specification<Patient> anySpecification() {
    return any(Specification.class);
  }

  @Test
  void getPatientsMapsPage() {
    Pageable pageable = PageRequest.of(0, 20);
    UUID id = UUID.randomUUID();
    when(patientRepository.findAll(anySpecification(), eq(pageable))).thenReturn(
        new PageImpl<>(List.of(patient(id, "a@b.com")), pageable, 1));

    PageResponse<PatientResponseDTO> result = service.getPatients(null, null,
        null, null, pageable);

    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().id()).isEqualTo(id.toString());
    assertThat(result.totalElements()).isEqualTo(1);
    assertThat(result.first()).isTrue();
    assertThat(result.last()).isTrue();
  }

  @Test
  void getPatientByIdMapsPatient() {
    UUID id = UUID.randomUUID();
    when(patientRepository.findById(id)).thenReturn(
        Optional.of(patient(id, "a@b.com")));

    PatientResponseDTO result = service.getPatientById(id);

    assertThat(result.id()).isEqualTo(id.toString());
    assertThat(result.email()).isEqualTo("a@b.com");
  }

  @Test
  void getPatientByIdThrowsWhenMissing() {
    UUID id = UUID.randomUUID();
    when(patientRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getPatientById(id)).isInstanceOf(
        PatientNotFoundException.class);
  }

  @Test
  void createPatientPersistsThenBillsThenPublishes() {
    UUID id = UUID.randomUUID();
    when(patientRepository.existsByEmail("a@b.com")).thenReturn(false);
    when(patientRepository.saveAndFlush(any(Patient.class))).thenReturn(
        patient(id, "a@b.com"));

    PatientResponseDTO result = service.createPatient(request("a@b.com"));

    assertThat(result.id()).isEqualTo(id.toString());
    assertThat(result.registeredDate()).isEqualTo(LocalDate.of(2024, 1, 10));
    InOrder order = inOrder(patientRepository, billingClient, kafkaProducer);
    order.verify(patientRepository).saveAndFlush(any(Patient.class));
    order.verify(billingClient).createBillingAccount(id.toString(), "John Doe",
        "a@b.com");
    order.verify(kafkaProducer).sendEvent(any(Patient.class));
  }

  @Test
  void createPatientRejectsDuplicateEmail() {
    when(patientRepository.existsByEmail("a@b.com")).thenReturn(true);

    assertThatThrownBy(() -> service.createPatient(request("a@b.com")))
        .isInstanceOf(EmailAlreadyExistsException.class)
        .hasMessageContaining("already exists: a@b.com");

    verify(patientRepository, never()).saveAndFlush(any());
  }

  @Test
  void createPatientPropagatesBillingFailureAndSkipsKafka() {
    when(patientRepository.saveAndFlush(any(Patient.class))).thenReturn(
        patient(UUID.randomUUID(), "a@b.com"));
    when(billingClient.createBillingAccount(any(), any(), any())).thenThrow(
        new BillingServiceException("down", true, null));

    assertThatThrownBy(() -> service.createPatient(request("a@b.com")))
        .isInstanceOf(BillingServiceException.class);

    verify(kafkaProducer, never()).sendEvent(any());
  }

  @Test
  void updatePatientChangesFieldsButNotRegisteredDate() {
    UUID id = UUID.randomUUID();
    Patient existing = patient(id, "old@b.com");
    when(patientRepository.findById(id)).thenReturn(Optional.of(existing));
    when(patientRepository.existsByEmailAndIdNot("new@b.com", id)).thenReturn(
        false);
    when(patientRepository.save(existing)).thenReturn(existing);

    PatientRequestDTO update = new PatientRequestDTO("Jane", "new@b.com",
        "2 Elm St", LocalDate.of(1990, 1, 1), LocalDate.of(2030, 1, 1));
    PatientResponseDTO result = service.updatePatient(id, update);

    assertThat(result.name()).isEqualTo("Jane");
    assertThat(result.email()).isEqualTo("new@b.com");
    assertThat(result.dateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
    assertThat(result.registeredDate()).isEqualTo(LocalDate.of(2024, 1, 10));
  }

  @Test
  void updatePatientThrowsWhenMissing() {
    UUID id = UUID.randomUUID();
    when(patientRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updatePatient(id, request("a@b.com")))
        .isInstanceOf(PatientNotFoundException.class);
  }

  @Test
  void updatePatientRejectsEmailOwnedByAnotherPatient() {
    UUID id = UUID.randomUUID();
    when(patientRepository.findById(id)).thenReturn(
        Optional.of(patient(id, "old@b.com")));
    when(patientRepository.existsByEmailAndIdNot("taken@b.com", id)).thenReturn(
        true);

    assertThatThrownBy(() -> service.updatePatient(id, request("taken@b.com")))
        .isInstanceOf(EmailAlreadyExistsException.class);
  }

  @Test
  void deletePatientRemovesExisting() {
    UUID id = UUID.randomUUID();
    when(patientRepository.existsById(id)).thenReturn(true);

    service.deletePatient(id);

    verify(patientRepository).deleteById(id);
  }

  @Test
  void deletePatientThrowsWhenMissing() {
    UUID id = UUID.randomUUID();
    when(patientRepository.existsById(id)).thenReturn(false);

    assertThatThrownBy(() -> service.deletePatient(id)).isInstanceOf(
        PatientNotFoundException.class);
    verify(patientRepository, never()).deleteById(any());
  }
}
