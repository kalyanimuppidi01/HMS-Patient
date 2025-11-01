package org.hms.patient.config;

import org.hms.patient.model.Patient;
import org.hms.patient.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

@Component
public class DataLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final PatientRepository repo;
    private final JdbcTemplate jdbc;

    public DataLoader(PatientRepository repo, JdbcTemplate jdbc) {
        this.repo = repo;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ClassPathResource resource = new ClassPathResource("seed/hms_patients.csv");
            if (!resource.exists()) {
                // fallback older name
                resource = new ClassPathResource("seed/patients.csv");
            }
            if (!resource.exists()) {
                log.info("Seed file not found: seed/hms_patients.csv or seed/patients.csv — skipping seed load");
                return;
            }

            List<Patient> toSave = new ArrayList<>();
            int total = 0, skipped = 0, added = 0;
            long maxIdSeen = 0L;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                boolean first = true;
                while ((line = br.readLine()) != null) {
                    // skip header if present
                    if (first && line.toLowerCase().contains("name") && line.toLowerCase().contains("email")) {
                        first = false;
                        continue;
                    }
                    first = false;
                    total++;

                    // naive CSV split — adjust if your CSV has quoted commas
                    String[] cols = line.split(",");
                    // Accept both variants: with patient_id (6 cols) or without (5 cols)
                    if (cols.length < 5) {
                        log.warn("Skipping malformed seed line (cols < 5): {}", line);
                        skipped++;
                        continue;
                    }

                    int idx = 0;
                    Long csvId = null;
                    String maybeId = cols[0].trim();
                    // If file contains headerless 6 columns, check whether first is numeric id or name
                    boolean firstIsId = false;
                    try {
                        // treat as id if it is a number
                        csvId = Long.parseLong(maybeId);
                        firstIsId = true;
                    } catch (NumberFormatException ignored) {
                        firstIsId = false;
                    }

                    String name, email, phone, dobStr;
                    if (firstIsId) {
                        // expected columns: id,name,email,phone,dob,created_at
                        if (cols.length < 6) {
                            log.warn("Skipping malformed seed line (expected 6 cols when id present): {}", line);
                            skipped++;
                            continue;
                        }
                        name = cols[1].trim();
                        email = cols[2].trim();
                        phone = cols[3].trim();
                        dobStr = cols[4].trim();
                    } else {
                        // columns: name,email,phone,dob,created_at
                        name = cols[0].trim();
                        email = cols[1].trim();
                        phone = cols[2].trim();
                        dobStr = cols[3].trim();
                    }

                    LocalDate dob = null;
                    try {
                        if (!dobStr.isEmpty()) dob = LocalDate.parse(dobStr);
                    } catch (Exception e) {
                        // ignore parse error, keep null
                    }

                    // dedupe by email or phone: if either exists, skip
                    boolean existsByEmail = email != null && !email.isBlank() && repo.existsByEmail(email);
                    boolean existsByPhone = phone != null && !phone.isBlank() && repo.existsByPhone(phone);
                    if (existsByEmail || existsByPhone) {
                        skipped++;
                        continue;
                    }

                    Patient p = new Patient();
                    if (csvId != null) {
                        p.setPatientId(csvId); // set explicit id — DB will accept explicit value
                        if (csvId > maxIdSeen) maxIdSeen = csvId;
                    }
                    p.setName(name);
                    p.setEmail(email);
                    p.setPhone(phone);
                    p.setDob(dob);
                    p.setActive(true);
                    p.setCreatedAt(OffsetDateTime.now());

                    toSave.add(p);

                    // batch save per 500 rows to avoid huge transactions
                    if (toSave.size() >= 500) {
                        saveBatch(toSave);
                        added += toSave.size();
                        toSave.clear();
                    }
                }
            }

            if (!toSave.isEmpty()) {
                saveBatch(toSave);
                added += toSave.size();
            }

            // if we saw explicit ids, update table AUTO_INCREMENT to maxIdSeen + 1
            if (maxIdSeen > 0) {
                long next = maxIdSeen + 1;
                try {
                    jdbc.execute("ALTER TABLE patients AUTO_INCREMENT = " + next);
                    log.info("Set patients AUTO_INCREMENT to {}", next);
                } catch (Exception e) {
                    log.warn("Failed to set AUTO_INCREMENT to {}: {}", next, e.getMessage());
                }
            } else {
                // if no ids provided, ensure AUTO_INCREMENT continues correctly
                OptionalLong maybeMax = repo.findAll().stream().mapToLong(Patient::getPatientId).max();
                if (maybeMax.isPresent()) {
                    long next = maybeMax.getAsLong() + 1;
                    try {
                        jdbc.execute("ALTER TABLE patients AUTO_INCREMENT = " + next);
                        log.info("Set patients AUTO_INCREMENT to {}", next);
                    } catch (Exception e) {
                        log.warn("Failed to set AUTO_INCREMENT to {}: {}", next, e.getMessage());
                    }
                }
            }

            log.info("Seed load finished. Total rows read: {}, added: {}, skipped (duplicates/malformed): {}",
                    total, added, skipped);

        } catch (Exception e) {
            log.error("Failed to load seed data — continuing startup (error logged)", e);
        }
    }

    @Transactional
    protected void saveBatch(List<Patient> list) {
        // use saveAll; wrapped in transaction
        repo.saveAll(list);
    }
}
