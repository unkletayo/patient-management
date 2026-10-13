package com.pm.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.model.Role;
import com.pm.authservice.model.User;
import com.pm.authservice.repository.UserRepository;
import com.pm.authservice.util.JwtUtil;
import com.pm.authservice.util.TokenClaims;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

  private static final String SECRET = Base64.getEncoder()
      .encodeToString("0123456789abcdef0123456789abcdef".getBytes());

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private JwtUtil jwtUtil;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = spy(new BCryptPasswordEncoder(4));
    jwtUtil = new JwtUtil(SECRET, 3_600_000L);
    authService = new AuthService(userRepository, passwordEncoder, jwtUtil);

    User user = new User();
    user.setEmail("user@test.com");
    user.setPassword(passwordEncoder.encode("password123"));
    user.setRole(Role.ADMIN);
    when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());
  }

  @Test
  void authenticateReturnsTokenCarryingEmailAndRole() {
    String token = authService.authenticate(new LoginRequestDTO("user@test.com", "password123"));

    assertThat(jwtUtil.validateToken(token)).isEqualTo(new TokenClaims("user@test.com", "ADMIN"));
  }

  @Test
  void authenticateRejectsWrongPassword() {
    assertThatThrownBy(
        () -> authService.authenticate(new LoginRequestDTO("user@test.com", "wrong-password")))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void authenticateRejectsUnknownUserButStillRunsEncoderMatch() {
    assertThatThrownBy(
        () -> authService.authenticate(new LoginRequestDTO("nobody@test.com", "password123")))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid email or password");

    verify(passwordEncoder).matches(org.mockito.ArgumentMatchers.eq("password123"), anyString());
  }

  @Test
  void validateTokenReturnsClaims() {
    String token = jwtUtil.generateToken("user@test.com", "USER");

    assertThat(authService.validateToken(token)).isEqualTo(new TokenClaims("user@test.com", "USER"));
  }

  @Test
  void validateTokenMapsInvalidTokenToBadCredentials() {
    assertThatThrownBy(() -> authService.validateToken("garbage"))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid or expired token");
  }
}
