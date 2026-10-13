package com.pm.patientservice.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pm.patientservice.dto.PageResponse;
import com.pm.patientservice.dto.PatientResponseDTO;
import com.pm.patientservice.exception.BillingServiceException;
import com.pm.patientservice.exception.EmailAlreadyExistsException;
import com.pm.patientservice.exception.GlobalExceptionHandler;
import com.pm.patientservice.exception.PatientNotFoundException;
import com.pm.patientservice.service.PatientService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PatientController.class)
@Import(GlobalExceptionHandler.class)
class PatientControllerTest {

  private static final String VALID_BODY = """
      {"name":"John Doe","email":"john@example.com","address":"1 Main St",
       "dateOfBirth":"1985-06-15"}""";

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  PatientService patientService;

  private static PatientResponseDTO dto(UUID id) {
    return new PatientResponseDTO(id.toString(), "John Doe",
        "john@example.com", "1 Main St", LocalDate.of(1985, 6, 15),
        LocalDate.of(2024, 1, 10));
  }

  private static PageResponse<PatientResponseDTO> page(PatientResponseDTO... items) {
    return new PageResponse<>(List.of(items), 0, 20, items.length, 1, true,
        true, items.length == 0);
  }

  private Pageable capturePageable() {
    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(patientService).getPatients(any(), any(), any(), any(),
        captor.capture());
    return captor.getValue();
  }

  @Test
  void listReturnsEnvelopeWithPage() throws Exception {
    UUID id = UUID.randomUUID();
    when(patientService.getPatients(any(), any(), any(), any(), any())).thenReturn(page(dto(id)));

    mockMvc.perform(get("/patients"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestSuccessful").value(true))
        .andExpect(jsonPath("$.responseMessage").value("success"))
        .andExpect(jsonPath("$.responseCode").value("0"))
        .andExpect(jsonPath("$.responseBody.content[0].id").value(id.toString()))
        .andExpect(jsonPath("$.responseBody.content[0].dateOfBirth").value("1985-06-15"))
        .andExpect(jsonPath("$.responseBody.content[0].registeredDate").value("2024-01-10"))
        .andExpect(jsonPath("$.responseBody.pageNumber").value(0))
        .andExpect(jsonPath("$.responseBody.totalElements").value(1))
        .andExpect(jsonPath("$.responseBody.first").value(true))
        .andExpect(jsonPath("$.responseBody.empty").value(false));

    Pageable pageable = capturePageable();
    assertThat(pageable.getPageNumber()).isZero();
    assertThat(pageable.getPageSize()).isEqualTo(20);
    assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
  }

  @Test
  void listWithoutFiltersPassesNulls() throws Exception {
    when(patientService.getPatients(any(), any(), any(), any(), any()))
        .thenReturn(page());

    mockMvc.perform(get("/patients")).andExpect(status().isOk());

    verify(patientService).getPatients(isNull(), isNull(), isNull(), isNull(),
        any(Pageable.class));
  }

  @Test
  void listPassesNameFilter() throws Exception {
    when(patientService.getPatients(any(), any(), any(), any(), any()))
        .thenReturn(page());

    mockMvc.perform(get("/patients").param("name", "jo"))
        .andExpect(status().isOk());

    verify(patientService).getPatients(eq("jo"), isNull(), isNull(), isNull(),
        any(Pageable.class));
  }

  @Test
  void listPassesEmailFilter() throws Exception {
    when(patientService.getPatients(any(), any(), any(), any(), any()))
        .thenReturn(page());

    mockMvc.perform(get("/patients").param("email", "@example.com"))
        .andExpect(status().isOk());

    verify(patientService).getPatients(isNull(), eq("@example.com"), isNull(),
        isNull(), any(Pageable.class));
  }

  @Test
  void listPassesRegisteredDateFilters() throws Exception {
    when(patientService.getPatients(any(), any(), any(), any(), any()))
        .thenReturn(page());

    mockMvc.perform(get("/patients").param("registeredFrom", "2024-01-01")
            .param("registeredTo", "2024-12-31"))
        .andExpect(status().isOk());

    verify(patientService).getPatients(isNull(), isNull(),
        eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 12, 31)),
        any(Pageable.class));
  }

  @Test
  void listPassesCombinedFiltersAndKeepsPaging() throws Exception {
    when(patientService.getPatients(any(), any(), any(), any(), any()))
        .thenReturn(page());

    mockMvc.perform(get("/patients").param("name", "jo").param("email", "ex")
            .param("registeredFrom", "2024-01-10")
            .param("registeredTo", "2024-01-10")
            .param("size", "5").param("sort", "email,desc"))
        .andExpect(status().isOk());

    ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
    verify(patientService).getPatients(eq("jo"), eq("ex"),
        eq(LocalDate.of(2024, 1, 10)), eq(LocalDate.of(2024, 1, 10)),
        captor.capture());
    assertThat(captor.getValue().getPageSize()).isEqualTo(5);
    assertThat(captor.getValue().getSort().getOrderFor("email")).isNotNull();
  }

  @Test
  void listNoMatchReturnsEmptyPage() throws Exception {
    when(patientService.getPatients(any(), any(), any(), any(), any()))
        .thenReturn(page());

    mockMvc.perform(get("/patients").param("name", "nobody"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestSuccessful").value(true))
        .andExpect(jsonPath("$.responseBody.content").isEmpty())
        .andExpect(jsonPath("$.responseBody.totalElements").value(0))
        .andExpect(jsonPath("$.responseBody.empty").value(true));
  }

  @Test
  void listUnparseableDateReturns400() throws Exception {
    mockMvc.perform(get("/patients").param("registeredFrom", "15/06/2024"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("400"))
        .andExpect(jsonPath("$.responseMessage")
            .value("Invalid value for parameter 'registeredFrom'"));

    verifyNoInteractions(patientService);
  }

  @Test
  void listFromAfterToReturns400() throws Exception {
    mockMvc.perform(get("/patients").param("registeredFrom", "2024-02-01")
            .param("registeredTo", "2024-01-31"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("400"))
        .andExpect(jsonPath("$.responseMessage")
            .value("registeredFrom must not be after registeredTo"));

    verifyNoInteractions(patientService);
  }

  @Test
  void listClampsPageSizeToMax() throws Exception {
    when(patientService.getPatients(any(), any(), any(), any(), any())).thenReturn(page());

    mockMvc.perform(get("/patients").param("size", "1000").param("page", "2"))
        .andExpect(status().isOk());

    Pageable pageable = capturePageable();
    assertThat(pageable.getPageSize()).isEqualTo(100);
    assertThat(pageable.getPageNumber()).isEqualTo(2);
  }

  @Test
  void listAcceptsWhitelistedSort() throws Exception {
    when(patientService.getPatients(any(), any(), any(), any(), any())).thenReturn(page());

    mockMvc.perform(get("/patients").param("sort", "dateOfBirth,desc"))
        .andExpect(status().isOk());

    Sort.Order order = capturePageable().getSort().getOrderFor("dateOfBirth");
    assertThat(order).isNotNull();
    assertThat(order.isDescending()).isTrue();
  }

  @Test
  void listRejectsUnknownSortField() throws Exception {
    mockMvc.perform(get("/patients").param("sort", "password,asc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("400"))
        .andExpect(jsonPath("$.responseBody").doesNotExist());
  }

  @Test
  void getByIdReturnsEnvelope() throws Exception {
    UUID id = UUID.randomUUID();
    when(patientService.getPatientById(id)).thenReturn(dto(id));

    mockMvc.perform(get("/patients/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestSuccessful").value(true))
        .andExpect(jsonPath("$.responseCode").value("0"))
        .andExpect(jsonPath("$.responseBody.id").value(id.toString()))
        .andExpect(jsonPath("$.responseBody.registeredDate").value("2024-01-10"));
  }

  @Test
  void getByIdMissingPatientReturns404() throws Exception {
    when(patientService.getPatientById(any())).thenThrow(
        new PatientNotFoundException("missing"));

    mockMvc.perform(get("/patients/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("404"));
  }

  @Test
  void getByIdBadUuidReturns400() throws Exception {
    mockMvc.perform(get("/patients/{id}", "not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.responseCode").value("400"));
  }

  @Test
  void createReturnsEnvelope() throws Exception {
    UUID id = UUID.randomUUID();
    when(patientService.createPatient(any())).thenReturn(dto(id));

    mockMvc.perform(post("/patients").contentType(MediaType.APPLICATION_JSON)
            .content(VALID_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestSuccessful").value(true))
        .andExpect(jsonPath("$.responseCode").value("0"))
        .andExpect(jsonPath("$.responseBody.id").value(id.toString()));
  }

  @Test
  void createValidationFailureReturnsFieldMap() throws Exception {
    mockMvc.perform(post("/patients").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"not-an-email\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("400"))
        .andExpect(jsonPath("$.responseBody.name").value("Name is required"))
        .andExpect(jsonPath("$.responseBody.email").value("Email should be valid"))
        .andExpect(jsonPath("$.responseBody.dateOfBirth").value("Date of birth is required"));
  }

  @Test
  void createMalformedJsonReturns400() throws Exception {
    mockMvc.perform(post("/patients").contentType(MediaType.APPLICATION_JSON)
            .content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.responseCode").value("400"))
        .andExpect(jsonPath("$.responseMessage").value("Malformed JSON request"));
  }

  @Test
  void createInvalidDateReturns400() throws Exception {
    mockMvc.perform(post("/patients").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"A","email":"a@b.com","address":"x",
                 "dateOfBirth":"15/06/1985"}"""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("400"));
  }

  @Test
  void createDuplicateEmailReturns409() throws Exception {
    when(patientService.createPatient(any())).thenThrow(
        new EmailAlreadyExistsException("dup"));

    mockMvc.perform(post("/patients").contentType(MediaType.APPLICATION_JSON)
            .content(VALID_BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("409"));
  }

  @Test
  void createBillingUnavailableReturns503() throws Exception {
    when(patientService.createPatient(any())).thenThrow(
        new BillingServiceException("down", true, null));

    mockMvc.perform(post("/patients").contentType(MediaType.APPLICATION_JSON)
            .content(VALID_BODY))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.responseCode").value("503"));
  }

  @Test
  void createBillingErrorReturns502() throws Exception {
    when(patientService.createPatient(any())).thenThrow(
        new BillingServiceException("bad", false, null));

    mockMvc.perform(post("/patients").contentType(MediaType.APPLICATION_JSON)
            .content(VALID_BODY))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.responseCode").value("502"));
  }

  @Test
  void updateMissingPatientReturns404() throws Exception {
    when(patientService.updatePatient(any(), any())).thenThrow(
        new PatientNotFoundException("missing"));

    mockMvc.perform(put("/patients/{id}", UUID.randomUUID())
            .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.responseCode").value("404"));
  }

  @Test
  void badUuidReturns400() throws Exception {
    mockMvc.perform(put("/patients/{id}", "not-a-uuid")
            .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.responseCode").value("400"));
  }

  @Test
  void deleteReturnsEnvelopeWithNullBody() throws Exception {
    mockMvc.perform(delete("/patients/{id}", UUID.randomUUID()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestSuccessful").value(true))
        .andExpect(jsonPath("$.responseCode").value("0"))
        .andExpect(jsonPath("$.responseBody").doesNotExist());
  }

  @Test
  void deleteMissingPatientReturns404() throws Exception {
    org.mockito.Mockito.doThrow(new PatientNotFoundException("missing"))
        .when(patientService).deletePatient(any());

    mockMvc.perform(delete("/patients/{id}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.responseCode").value("404"));
  }

  @Test
  void unexpectedErrorReturnsGeneric500() throws Exception {
    when(patientService.getPatients(any(), any(), any(), any(), any())).thenThrow(
        new IllegalStateException("secret internals"));

    mockMvc.perform(get("/patients"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.responseCode").value("500"))
        .andExpect(jsonPath("$.responseMessage").value("Internal server error"));
  }

  @Test
  void unknownRouteReturns404Envelope() throws Exception {
    mockMvc.perform(get("/nope"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("404"));
  }
}
