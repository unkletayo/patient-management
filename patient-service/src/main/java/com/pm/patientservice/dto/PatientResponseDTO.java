package com.pm.patientservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record PatientResponseDTO(
    String id,
    String name,
    String email,
    String address,
    @Schema(type = "string", format = "date", example = "1985-06-15")
    LocalDate dateOfBirth,
    @Schema(type = "string", format = "date", example = "2024-01-10")
    LocalDate registeredDate) {
}
