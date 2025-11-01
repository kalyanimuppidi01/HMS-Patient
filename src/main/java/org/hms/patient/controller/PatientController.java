package org.hms.patient.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hms.patient.dto.PaginationResponse;
import org.hms.patient.exception.BadRequestException;
import org.hms.patient.exception.ResourceNotFoundException;
import org.hms.patient.model.Patient;
import org.hms.patient.service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "Patient API", description = "CRUD and search for patients")
@RestController
@RequestMapping("/v1/patients")
public class PatientController {
    @Autowired
    private PatientService service;

    @Operation(summary = "Create a patient")
    @PostMapping
    public ResponseEntity<Patient> create(@RequestBody Patient p) {
        if (p.getEmail() == null || p.getPhone() == null) {
            throw new BadRequestException("email and phone are required");
        }
        Patient saved = service.create(p);
        return ResponseEntity.created(URI.create("/v1/patients/" + saved.getPatientId())).body(saved);
    }

    @Operation(summary = "List patients (paginated)")
    @GetMapping
    public PaginationResponse<Patient> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        Page<Patient> p = service.list(page, size);
        return new PaginationResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Operation(summary = "Get a patient by id")
    @GetMapping("/{id}")
    public ResponseEntity<Patient> get(@PathVariable Long id) {
        return service.get(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + id));
    }

    @Operation(summary = "Update a patient")
    @PutMapping("/{id}")
    public Patient update(@PathVariable Long id, @RequestBody Patient p) {
        return service.update(id, p);
    }

    @Operation(summary = "Delete (deactivate) a patient")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search patients by name or phone")
    @GetMapping("/search")
    public PaginationResponse<Patient> search(@RequestParam(required = false) String name,
                                              @RequestParam(required = false) String phone,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        Page<Patient> p;
        if (name != null) p = service.searchByName(name, page, size);
        else if (phone != null) p = service.searchByPhone(phone, page, size);
        else p = service.list(page, size);

        return new PaginationResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Operation(summary = "Check if patient exists and active")
    @GetMapping("/{id}/exists")
    public ResponseEntity<?> exists(@PathVariable Long id) {
        return service.get(id).map(p -> ResponseEntity.ok().body(java.util.Map.of("exists", true, "active", p.isActive())))
                .orElseGet(() -> ResponseEntity.ok().body(java.util.Map.of("exists", false)));
    }
}
