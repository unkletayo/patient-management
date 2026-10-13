package com.pm.patientservice.service;

import com.pm.patientservice.dto.PageResponse;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.grpc.BillingServiceGrpcClient;
import com.pm.patientservice.kafka.KafkaProducer;
import com.pm.patientservice.mapper.PatientMapper;
import com.pm.patientservice.model.Patient;
import com.pm.patientservice.repository.PatientRepository;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

  private final PatientRepository patientRepository;
  private final BillingServiceGrpcClient billingServiceGrpcClient;
  private final KafkaProducer kafkaProducer;

  public PatientService(PatientRepository patientRepository,
      BillingServiceGrpcClient billingServiceGrpcClient,
      KafkaProducer kafkaProducer) {
    this.patientRepository = patientRepository;
    this.billingServiceGrpcClient = billingServiceGrpcClient;
    this.kafkaProducer = kafkaProducer;
  }

  @Transactional(readOnly = true)
  public PageResponse<PatientResponseDTO> getPatients(Pageable pageable) {
    return PageResponse.from(patientRepository.findAll(pageable),
        PatientMapper::toDTO);
  }

  /**
   * Persists the patient, creates the billing account and publishes the event.
   * A billing failure rolls the transaction back so no orphan patient remains.
   */
  @Transactional
  public PatientResponseDTO createPatient(PatientRequestDTO request) {
    if (patientRepository.existsByEmail(request.email())) {
      throw emailExists(request.email());
    }

    Patient newPatient = patientRepository.saveAndFlush(
        PatientMapper.toModel(request));

    billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(),
        newPatient.getName(), newPatient.getEmail());

    kafkaProducer.sendEvent(newPatient);

    return PatientMapper.toDTO(newPatient);
  }

  @Transactional
  public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO request) {
    Patient patient = findOrThrow(id);

    if (patientRepository.existsByEmailAndIdNot(request.email(), id)) {
      throw emailExists(request.email());
    }

    patient.setName(request.name());
    patient.setAddress(request.address());
    patient.setEmail(request.email());
    patient.setDateOfBirth(request.dateOfBirth());

    return PatientMapper.toDTO(patientRepository.save(patient));
  }

  @Transactional
  public void deletePatient(UUID id) {
    if (!patientRepository.existsById(id)) {
      throw notFound(id);
    }
    patientRepository.deleteById(id);
  }

  private Patient findOrThrow(UUID id) {
    return patientRepository.findById(id).orElseThrow(() -> notFound(id));
  }

  private static PatientNotFoundException notFound(UUID id) {
    return new PatientNotFoundException("Patient not found with ID: " + id);
  }

  private static EmailAlreadyExistsException emailExists(String email) {
    return new EmailAlreadyExistsException(
        "A patient with this email already exists: " + email);
  }
}
