package com.pm.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

  private static final String[] PUBLIC_ENDPOINTS = {
      "/login",
      "/validate",
      "/v3/api-docs/**",
      "/swagger-ui/**",
      "/swagger-ui.html",
      "/actuator/health"
  };

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      JsonErrorHandler errorHandler) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
            .anyRequest().denyAll())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Stateless token API without cookies/sessions: CSRF protection does not apply
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .exceptionHandling(handling -> handling
            .authenticationEntryPoint(errorHandler)
            .accessDeniedHandler(errorHandler));

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
