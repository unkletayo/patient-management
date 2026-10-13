package com.pm.authservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm.authservice.model.Role;
import com.pm.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

  @Autowired
  private UserRepository userRepository;

  @Test
  void contextLoadsAndSeedUserIsPresent() {
    var user = userRepository.findByEmail("testuser@test.com");

    assertThat(user).isPresent();
    assertThat(user.get().getRole()).isEqualTo(Role.ADMIN);
  }
}
