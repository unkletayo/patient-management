package com.pm.patientservice.controller;

import com.pm.patientservice.dto.ApiResponse;
import com.pm.patientservice.dto.PageResponse;
import com.pm.patientservice.dto.PatientRequestDTO;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.InvalidSortException;
import com.pm.patientservice.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/patients")
@Tag(name = "Patient", description = "API for managing Patients")
@ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500",
        description = "Unexpected error",
        content = @Content(schema = @Schema(implementation = ApiResponse.class)))
})
public class PatientController {

  static final Set<String> SORTABLE_FIELDS = Set.of("name", "email",
      "dateOfBirth", "registeredDate");

  private final PatientService patientService;

  public PatientController(PatientService patientService) {
    this.patientService = patientService;
  }

  @GetMapping
  @Operation(summary = "Get Patients",
      description = "Paginated list. Query params: page (0-based), size "
          + "(default 20, max 100) and sort=field,asc|desc where field is one "
          + "of name, email, dateOfBirth, registeredDate.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
      description = "Page of patients wrapped in the standard envelope")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
      description = "Invalid sort field or parameter",
      content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  public ResponseEntity<ApiResponse<PageResponse<PatientResponseDTO>>> getPatients(
      @Parameter(hidden = true)
      @PageableDefault(size = 20)
      @SortDefault(sort = "name") Pageable pageable) {
    validateSort(pageable);
    return ResponseEntity.ok(
        ApiResponse.success(patientService.getPatients(pageable)));
  }

  @PostMapping
  @Operation(summary = "Create a new Patient")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
      description = "Patient created")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
      description = "Validation failure (responseBody is a field -> message map)",
      content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
      description = "Email already registered",
      content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502",
      description = "Billing service returned an error",
      content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
      description = "Billing service unavailable or timed out",
      content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  public ResponseEntity<ApiResponse<PatientResponseDTO>> createPatient(
      @Valid @RequestBody PatientRequestDTO patientRequestDTO) {
    return ResponseEntity.ok(
        ApiResponse.success(patientService.createPatient(patientRequestDTO)));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a Patient",
      description = "registeredDate in the body is ignored.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
      description = "Patient updated")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
      description = "Validation failure or malformed id",
      content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
      description = "Patient not found",
      content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
      description = "Email already registered",
      content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  public ResponseEntity<ApiResponse<PatientResponseDTO>> updatePatient(
      @PathVariable UUID id,
      @Valid @RequestBody PatientRequestDTO patientRequestDTO) {
    return ResponseEntity.ok(
        ApiResponse.success(patientService.updatePatient(id, patientRequestDTO)));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete a Patient")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
      description = "Patient deleted (responseBody is null)")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
      description = "Patient not found",
      content = @Content(schema = @Schema(implementation = ApiResponse.class)))
  public ResponseEntity<ApiResponse<Void>> deletePatient(
      @PathVariable UUID id) {
    patientService.deletePatient(id);
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  private static void validateSort(Pageable pageable) {
    pageable.getSort().forEach(order -> {
      if (!SORTABLE_FIELDS.contains(order.getProperty())) {
        throw new InvalidSortException("Cannot sort by '" + order.getProperty()
            + "'. Allowed fields: " + SORTABLE_FIELDS.stream().sorted().toList());
      }
    });
  }
}
