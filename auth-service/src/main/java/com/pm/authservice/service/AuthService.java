package com.pm.authservice.service;

import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.repository.UserRepository;
import com.pm.authservice.util.JwtUtil;
import com.pm.authservice.util.TokenClaims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private static final String INVALID_CREDENTIALS = "Invalid email or password";
  private static final String INVALID_TOKEN = "Invalid or expired token";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  // Matched against when the user is unknown so the response time does not reveal
  // whether an account exists.
  private final String dummyPasswordHash;

  public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
      JwtUtil jwtUtil) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtil = jwtUtil;
    this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
  }

  /**
   * @return a signed JWT for the given credentials
   * @throws BadCredentialsException when the email is unknown or the password is wrong
   */
  public String authenticate(LoginRequestDTO loginRequest) {
    var user = userRepository.findByEmail(loginRequest.email());

    String hash = user.map(u -> u.getPassword()).orElse(dummyPasswordHash);
    boolean passwordMatches = passwordEncoder.matches(loginRequest.password(), hash);

    if (user.isEmpty() || !passwordMatches) {
      throw new BadCredentialsException(INVALID_CREDENTIALS);
    }
    return jwtUtil.generateToken(user.get().getEmail(), user.get().getRole().name());
  }

  /**
   * @throws BadCredentialsException when the token is invalid or expired
   */
  public TokenClaims validateToken(String token) {
    try {
      return jwtUtil.validateToken(token);
    } catch (JwtException e) {
      throw new BadCredentialsException(INVALID_TOKEN);
    }
  }
}
