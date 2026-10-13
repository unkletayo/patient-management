package com.pm.patientservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.pm.patientservice.model.Patient;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class PatientSpecificationsTest {

  @Autowired
  PatientRepository repository;

  @BeforeEach
  void seed() {
    repository.deleteAll();
    save("John Doe", "john@example.com", LocalDate.of(2024, 1, 10));
    save("Jane Smith", "jane@mail.org", LocalDate.of(2024, 3, 5));
    save("100% Real_Name", "real@odd.com", LocalDate.of(2024, 6, 1));
    save("Back\\slash", "back@odd.com", LocalDate.of(2024, 6, 30));
  }

  private void save(String name, String email, LocalDate registered) {
    Patient p = new Patient();
    p.setName(name);
    p.setEmail(email);
    p.setAddress("1 Main St");
    p.setDateOfBirth(LocalDate.of(1985, 6, 15));
    p.setRegisteredDate(registered);
    repository.saveAndFlush(p);
  }

  private List<String> names(String name, String email, LocalDate from,
      LocalDate to) {
    return repository.findAll(
            PatientSpecifications.withFilters(name, email, from, to),
            PageRequest.of(0, 20, Sort.by("name")))
        .map(Patient::getName).getContent();
  }

  @Test
  void noFilterReturnsAll() {
    assertThat(names(null, null, null, null)).hasSize(4);
  }

  @Test
  void blankTextFiltersAreIgnored() {
    assertThat(names("  ", "", null, null)).hasSize(4);
  }

  @Test
  void nameMatchesCaseInsensitiveContains() {
    assertThat(names("OHN d", null, null, null)).containsExactly("John Doe");
  }

  @Test
  void emailMatchesCaseInsensitiveContains() {
    assertThat(names(null, "MAIL.ORG", null, null))
        .containsExactly("Jane Smith");
  }

  @Test
  void registeredFromIsInclusive() {
    assertThat(names(null, null, LocalDate.of(2024, 3, 5), null))
        .containsExactlyInAnyOrder("Jane Smith", "100% Real_Name",
            "Back\\slash");
  }

  @Test
  void registeredToIsInclusive() {
    assertThat(names(null, null, null, LocalDate.of(2024, 3, 5)))
        .containsExactlyInAnyOrder("Jane Smith", "John Doe");
  }

  @Test
  void singleDayRangeMatchesThatDay() {
    LocalDate day = LocalDate.of(2024, 1, 10);
    assertThat(names(null, null, day, day)).containsExactly("John Doe");
  }

  @Test
  void combinedFiltersAreAnded() {
    assertThat(names("j", "example", LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 2, 1))).containsExactly("John Doe");
    assertThat(names("jane", "example", null, null)).isEmpty();
  }

  @Test
  void noMatchReturnsEmptyPage() {
    assertThat(names("nobody", null, null, null)).isEmpty();
  }

  @Test
  void percentIsTreatedAsLiteral() {
    assertThat(names("%", null, null, null)).containsExactly("100% Real_Name");
  }

  @Test
  void underscoreIsTreatedAsLiteral() {
    assertThat(names("_", null, null, null)).containsExactly("100% Real_Name");
    assertThat(names("Jo_n", null, null, null)).isEmpty();
  }

  @Test
  void backslashIsTreatedAsLiteral() {
    assertThat(names("\\", null, null, null)).containsExactly("Back\\slash");
  }

  @Test
  void escapeLikePrefixesWildcards() {
    assertThat(PatientSpecifications.escapeLike("a%b_c\\d"))
        .isEqualTo("a\\%b\\_c\\\\d");
    assertThat(PatientSpecifications.containsPattern(" Jo%N "))
        .isEqualTo("%jo\\%n%");
  }
}
