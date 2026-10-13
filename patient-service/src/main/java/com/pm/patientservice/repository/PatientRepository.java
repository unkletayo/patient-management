package com.pm.patientservice.repository;

import com.pm.patientservice.model.Patient;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID>,
    JpaSpecificationExecutor<Patient> {
  boolean existsByEmail(String email);
  boolean existsByEmailAndIdNot(String email, UUID id);

  @Override
  Page<Patient> findAll(Pageable pageable);
}
