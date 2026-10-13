package com.pm.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pm.authservice.config.JsonErrorHandler;
import com.pm.authservice.config.SecurityConfig;
import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.service.AuthService;
import com.pm.authservice.util.TokenClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JsonErrorHandler.class})
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @Test
  void loginReturnsTokenInEnvelope() throws Exception {
    when(authService.authenticate(any(LoginRequestDTO.class))).thenReturn("jwt-token");

    mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"testuser@test.com\",\"password\":\"password123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestSuccessful").value(true))
        .andExpect(jsonPath("$.responseMessage").value("success"))
        .andExpect(jsonPath("$.responseCode").value("0"))
        .andExpect(jsonPath("$.responseBody.token").value("jwt-token"));
  }

  @Test
  void loginWithBadCredentialsReturns401Envelope() throws Exception {
    when(authService.authenticate(any(LoginRequestDTO.class)))
        .thenThrow(new BadCredentialsException("Invalid email or password"));

    mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"testuser@test.com\",\"password\":\"wrong-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseMessage").value("Invalid email or password"))
        .andExpect(jsonPath("$.responseCode").value("401"));
  }

  @Test
  void loginWithInvalidBodyReturns400WithFieldMap() throws Exception {
    mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("400"))
        .andExpect(jsonPath("$.responseBody.email").exists())
        .andExpect(jsonPath("$.responseBody.password").exists());
  }

  @Test
  void loginWithMalformedJsonReturns400Envelope() throws Exception {
    mockMvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON).content("{not json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("400"));
  }

  @Test
  void validateReturnsEmailAndRole() throws Exception {
    when(authService.validateToken("good-token"))
        .thenReturn(new TokenClaims("testuser@test.com", "ADMIN"));

    mockMvc.perform(get("/validate").header(HttpHeaders.AUTHORIZATION, "Bearer good-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestSuccessful").value(true))
        .andExpect(jsonPath("$.responseCode").value("0"))
        .andExpect(jsonPath("$.responseBody.email").value("testuser@test.com"))
        .andExpect(jsonPath("$.responseBody.role").value("ADMIN"));
  }

  @Test
  void validateWithoutHeaderReturns401Envelope() throws Exception {
    mockMvc.perform(get("/validate"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("401"));
  }

  @Test
  void validateWithMalformedHeaderReturns401Envelope() throws Exception {
    mockMvc.perform(get("/validate").header(HttpHeaders.AUTHORIZATION, "Basic abc"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.responseCode").value("401"));
  }

  @Test
  void validateWithInvalidTokenReturns401Envelope() throws Exception {
    when(authService.validateToken("bad-token"))
        .thenThrow(new BadCredentialsException("Invalid or expired token"));

    mockMvc.perform(get("/validate").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseMessage").value("Invalid or expired token"))
        .andExpect(jsonPath("$.responseCode").value("401"));
  }

  @Test
  void unlistedEndpointsAreDeniedWithEnvelope() throws Exception {
    mockMvc.perform(get("/admin/anything"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.requestSuccessful").value(false))
        .andExpect(jsonPath("$.responseCode").value("401"));
  }
}
