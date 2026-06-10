package com.alhilal.customer;

import com.alhilal.customer.api.AuthCredential;
import com.alhilal.customer.api.AuthRepository;
import com.alhilal.customer.domain.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@SpringBootApplication
public class CustomerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}


@Component
@RequiredArgsConstructor
@Slf4j
class CustomerDataSeeder implements CommandLineRunner {

    private final CustomerService customerService;
    private final AuthRepository authRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            // Seed customer 1 — Abdullah (KYC verified, with credentials)
            var abdullah = customerService.register(
                    "Abdullah", "Al Rashidi",
                    "abdullah@test.com", "+971501234567",
                    "784-1990-1234567-1"
            );
            customerService.verifyKyc(abdullah.getCustomerId());

            // Create auth credentials for Abdullah
            if (authRepository.findByEmail("abdullah@test.com").isEmpty()) {
                AuthCredential cred = new AuthCredential();
                cred.setCustomerId(abdullah.getCustomerId());
                cred.setEmail("abdullah@test.com");
                cred.setPasswordHash(passwordEncoder.encode("password123"));
                cred.setRoles(List.of("ROLE_CUSTOMER"));
                cred.setCreatedAt(java.time.LocalDateTime.now());
                authRepository.save(cred);
            }

            // Seed customer 2 — Fatima (KYC pending, with credentials)
            var fatima = customerService.register(
                    "Fatima", "Al Zaabi",
                    "fatima@test.com", "+971507654321",
                    "784-1995-7654321-1"
            );

            if (authRepository.findByEmail("fatima@test.com").isEmpty()) {
                AuthCredential cred = new AuthCredential();
                cred.setCustomerId(fatima.getCustomerId());
                cred.setEmail("fatima@test.com");
                cred.setPasswordHash(passwordEncoder.encode("password456"));
                cred.setRoles(List.of("ROLE_CUSTOMER"));
                cred.setCreatedAt(java.time.LocalDateTime.now());
                authRepository.save(cred);
            }

            log.info("=== Demo customers seeded ===");
            log.info("Abdullah: {} / password123", abdullah.getCustomerId());
            log.info("Fatima:   {} / password456", fatima.getCustomerId());
            log.info("Login: POST http://localhost:8080/api/auth/login");

        } catch (Exception e) {
            log.warn("Seeding skipped (probably already exists): {}", e.getMessage());
        }
    }
}