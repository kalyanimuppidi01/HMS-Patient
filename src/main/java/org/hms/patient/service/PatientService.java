package org.hms.patient.service;

import org.hms.patient.model.Patient;
import org.hms.patient.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PatientService {
    @Autowired
    private PatientRepository repo;

    public Patient create(Patient p) { return repo.save(p); }

    public Page<Patient> list(int page, int size) {
        return repo.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    public Optional<Patient> get(Long id) { return repo.findById(id); }

    public Patient update(Long id, Patient updated) {
        return repo.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setEmail(updated.getEmail());
            existing.setPhone(updated.getPhone());
            existing.setDob(updated.getDob());
            existing.setActive(updated.isActive());
            return repo.save(existing);
        }).orElseThrow(() -> new RuntimeException("PATIENT_NOT_FOUND"));
    }

    public void delete(Long id) {
        repo.findById(id).ifPresent(p -> { p.setActive(false); repo.save(p); });
    }

    public Page<Patient> searchByName(String name, int page, int size) {
        return repo.findByNameContainingIgnoreCase(name, PageRequest.of(page, size));
    }

    public Page<Patient> searchByPhone(String phone, int page, int size) {
        return repo.findByPhoneContaining(phone, PageRequest.of(page, size));
    }
}
