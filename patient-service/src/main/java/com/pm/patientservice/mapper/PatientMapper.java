package com.pm.patientservice.mapper;

import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.model.Patient;
import java.time.LocalDate;

public final class PatientMapper {

  private PatientMapper() {
  }

  public static PatientResponseDTO toDTO(Patient patient) {
    return new PatientResponseDTO(
        patient.getId().toString(),
        patient.getName(),
        patient.getEmail(),
        patient.getAddress(),
        patient.getDateOfBirth(),
        patient.getRegisteredDate());
  }

  public static Patient toModel(PatientRequestDTO request) {
    Patient patient = new Patient();
    patient.setName(request.name());
    patient.setAddress(request.address());
    patient.setEmail(request.email());
    patient.setDateOfBirth(request.dateOfBirth());
    patient.setRegisteredDate(
        request.registeredDate() != null ? request.registeredDate()
            : LocalDate.now());
    return patient;
  }
}
