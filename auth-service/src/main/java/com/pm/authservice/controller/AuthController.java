package com.pm.authservice.controller;

import com.pm.authservice.dto.ApiResponse;
import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.dto.LoginResponseDTO;
import com.pm.authservice.dto.ValidateResponseDTO;
import com.pm.authservice.service.AuthService;
import com.pm.authservice.util.TokenClaims;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

  private static final String BEARER_PREFIX = "Bearer ";

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @Operation(summary = "Generate token on user login")
  @PostMapping("/login")
  public ApiResponse<LoginResponseDTO> login(
      @Valid @RequestBody LoginRequestDTO loginRequestDTO) {
    String token = authService.authenticate(loginRequestDTO);
    return ApiResponse.success(new LoginResponseDTO(token));
  }

  @Operation(summary = "Validate Token")
  @GetMapping("/validate")
  public ApiResponse<ValidateResponseDTO> validateToken(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

    // Authorization: Bearer <token>
    if (authHeader == null || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0,
        BEARER_PREFIX.length())) {
      throw new BadCredentialsException("Missing or malformed Authorization header");
    }

    TokenClaims claims = authService.validateToken(
        authHeader.substring(BEARER_PREFIX.length()).trim());
    return ApiResponse.success(new ValidateResponseDTO(claims.email(), claims.role()));
  }
}
