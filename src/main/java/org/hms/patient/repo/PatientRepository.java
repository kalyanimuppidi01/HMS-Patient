package org.hms.patient.repository;

import org.hms.patient.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Page<Patient> findByNameContainingIgnoreCase(String name, Pageable p);
    Page<Patient> findByPhoneContaining(String phone, Pageable p);
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
    Optional<Patient> findTopByOrderByPatientIdDesc();

}
