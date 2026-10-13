package com.pm.billingservice.controller;

import com.pm.billingservice.dto.ApiResponse;
import com.pm.billingservice.dto.BillingAccountResponse;
import com.pm.billingservice.dto.PageResponse;
import com.pm.billingservice.dto.UpdateBillingAccountStatusRequest;
import com.pm.billingservice.service.BillingAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/billing-accounts")
@Validated
@Tag(name = "Billing", description = "API for reading billing accounts and changing their status")
public class BillingAccountController {

  private final BillingAccountService billingAccountService;

  public BillingAccountController(BillingAccountService billingAccountService) {
    this.billingAccountService = billingAccountService;
  }

  @GetMapping
  @Operation(summary = "List billing accounts")
  public ResponseEntity<ApiResponse<PageResponse<BillingAccountResponse>>> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

    PageResponse<BillingAccountResponse> body = PageResponse.from(
        billingAccountService.list(page, size), BillingAccountResponse::from);
    return ResponseEntity.ok(ApiResponse.success(body));
  }

  @GetMapping("/{patientId}")
  @Operation(summary = "Get a billing account by patient id")
  public ResponseEntity<ApiResponse<BillingAccountResponse>> getByPatientId(
      @PathVariable String patientId) {

    BillingAccountResponse body = BillingAccountResponse.from(
        billingAccountService.getByPatientId(patientId));
    return ResponseEntity.ok(ApiResponse.success(body));
  }

  @PatchMapping("/{patientId}/status")
  @Operation(summary = "Update the status of a billing account")
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
          description = "Status updated (or already set)"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
          description = "Missing or unknown status"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
          description = "Billing account not found"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
          description = "Status transition not allowed")
  })
  public ResponseEntity<ApiResponse<BillingAccountResponse>> updateStatus(
      @PathVariable String patientId,
      @Valid @RequestBody UpdateBillingAccountStatusRequest request) {

    BillingAccountResponse body = BillingAccountResponse.from(
        billingAccountService.updateStatus(patientId, request.status()));
    return ResponseEntity.ok(ApiResponse.success(body));
  }
}
