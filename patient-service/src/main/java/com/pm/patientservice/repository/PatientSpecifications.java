package com.pm.patientservice.repository;

import com.pm.patientservice.model.Patient;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class PatientSpecifications {

  private static final char ESCAPE = '\\';

  private PatientSpecifications() {
  }

  /**
   * Builds an AND-combined filter. Null or blank values are ignored, name and
   * email match case-insensitively as substrings, and both dates are inclusive.
   */
  public static Specification<Patient> withFilters(String name, String email,
      LocalDate registeredFrom, LocalDate registeredTo) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (name != null && !name.isBlank()) {
        predicates.add(cb.like(cb.lower(root.get("name")), containsPattern(name),
            ESCAPE));
      }
      if (email != null && !email.isBlank()) {
        predicates.add(cb.like(cb.lower(root.get("email")),
            containsPattern(email), ESCAPE));
      }
      if (registeredFrom != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("registeredDate"),
            registeredFrom));
      }
      if (registeredTo != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("registeredDate"),
            registeredTo));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  static String containsPattern(String value) {
    return "%" + escapeLike(value.trim().toLowerCase(Locale.ROOT)) + "%";
  }

  static String escapeLike(String value) {
    StringBuilder escaped = new StringBuilder(value.length());
    for (char c : value.toCharArray()) {
      if (c == ESCAPE || c == '%' || c == '_') {
        escaped.append(ESCAPE);
      }
      escaped.append(c);
    }
    return escaped.toString();
  }
}
