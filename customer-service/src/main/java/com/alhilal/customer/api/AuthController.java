package com.alhilal.customer.api;

import com.alhilal.customer.domain.Customer;
import com.alhilal.customer.domain.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ================================================================
 * AUTH CONTROLLER — Login and get JWT token
 * ================================================================
 * In production: this would be ForgeRock or a separate auth-service
 * In demo: lives inside customer-service for simplicity
 *
 * PUBLIC endpoints — no JWT required to call these:
 *   POST /api/auth/login    ← get token
 *   POST /api/auth/register ← create account + get token
 *
 * Flow:
 *   1. Client sends email + password
 *   2. We find customer by email
 *   3. We verify BCrypt password
 *   4. We generate JWT with customerId + roles
 *   5. Client gets token
 *   6. Client uses token on all future requests
 * ================================================================
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login and get JWT token")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthRepository authRepository;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder;

    // ----------------------------------------------------------------
    // REGISTER — create customer + get token immediately
    // ----------------------------------------------------------------
    @PostMapping("/register")
    @Operation(
            summary = "Register new customer and get JWT token",
            description = """
            Creates customer account and returns JWT token immediately.
            
            Flow:
            1. Validate input
            2. Check email not already taken
            3. Hash password with BCrypt
            4. Save customer
            5. Generate JWT token
            6. Return token — ready to use immediately
            """
    )
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {

        // Check email already exists
        if (authRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
        }

        // Hash password — NEVER store plain text
        String passwordHash = passwordEncoder.encode(request.password());

        // Save credentials
        AuthCredential credential = new AuthCredential();
        credential.setCustomerId("CUST-" +
                java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        credential.setEmail(request.email());
        credential.setPasswordHash(passwordHash);
        credential.setRoles(List.of("ROLE_CUSTOMER"));
        credential.setCreatedAt(LocalDateTime.now());
        authRepository.save(credential);

        // Generate JWT immediately
        String token = jwtUtils.generateToken(
                credential.getCustomerId(),
                credential.getRoles()
        );

        log.info("[AUTH] Registered new customer: {}", credential.getCustomerId());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new TokenResponse(
                        token,
                        credential.getCustomerId(),
                        credential.getEmail(),
                        credential.getRoles(),
                        "15 minutes",
                        "Registration successful. Use this token for all API calls."
                )
        );
    }

    // ----------------------------------------------------------------
    // LOGIN — verify credentials + get token
    // ----------------------------------------------------------------
    @PostMapping("/login")
    @Operation(
            summary = "Login and get JWT token",
            description = """
            Verifies email/password and returns JWT token.
            
            Flow:
            1. Find customer by email
            2. Verify BCrypt password
            3. Check account not locked
            4. Generate JWT { sub: customerId, roles, exp: +15min }
            5. Return token
            
            Use the token in all subsequent requests:
            Authorization: Bearer <token>
            """
    )
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("[AUTH] Login attempt for email: {}", request.email());

        // Step 1: find by email
        Optional<AuthCredential> credOpt = authRepository.findByEmail(request.email());
        if (credOpt.isEmpty()) {
            // Don't reveal whether email exists (security best practice)
            log.warn("[AUTH] Login failed — email not found: {}", request.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        AuthCredential credential = credOpt.get();

        // Step 2: check account not locked
        if (credential.isAccountLocked()) {
            log.warn("[AUTH] Login failed — account locked: {}", request.email());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Account locked. Contact support."));
        }

        // Step 3: verify password using BCrypt
        // BCrypt automatically handles the salt — just compare
        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {

            // Increment failed attempts
            credential.setFailedAttempts(credential.getFailedAttempts() + 1);
            // Lock after 5 failed attempts
            if (credential.getFailedAttempts() >= 5) {
                credential.setAccountLocked(true);
                log.warn("[AUTH] Account locked after 5 failed attempts: {}", request.email());
            }
            authRepository.save(credential);

            log.warn("[AUTH] Login failed — wrong password for: {}", request.email());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password"));
        }

        // Step 4: reset failed attempts on successful login
        credential.setFailedAttempts(0);
        credential.setLastLoginAt(LocalDateTime.now());
        authRepository.save(credential);

        // Step 5: generate JWT token
        String token = jwtUtils.generateToken(
                credential.getCustomerId(),
                credential.getRoles()
        );

        log.info("[AUTH] Login successful for: {}", credential.getCustomerId());

        // Step 6: return token
        return ResponseEntity.ok(new TokenResponse(
                token,
                credential.getCustomerId(),
                credential.getEmail(),
                credential.getRoles(),
                "15 minutes",
                "Login successful. Add to requests: Authorization: Bearer <token>"
        ));
    }

    // ----------------------------------------------------------------
    // VALIDATE — check if a token is valid (called by Kong)
    // ----------------------------------------------------------------
    @GetMapping("/validate")
    @Operation(
            summary = "Validate a JWT token",
            description = "Returns token claims if valid. Called by Kong to verify tokens."
    )
    public ResponseEntity<?> validate(
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7); // remove "Bearer "
            var claims = jwtUtils.validateAndExtract(token);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "customerId", claims.getSubject(),
                    "roles", claims.get("roles"),
                    "expiresAt", claims.getExpiration()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", e.getMessage()));
        }
    }
}

// ----------------------------------------------------------------
// REQUEST / RESPONSE RECORDS
// ----------------------------------------------------------------
record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {}

record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String mobileNumber,
        String emiratesId
) {}

record TokenResponse(
        String token,
        String customerId,
        String email,
        List<String> roles,
        String expiresIn,
        String message
) {}