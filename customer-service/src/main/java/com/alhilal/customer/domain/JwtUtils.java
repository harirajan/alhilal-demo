package com.alhilal.customer.domain;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * ================================================================
 * JWT UTILITY
 * ================================================================
 * Generates and validates JWT tokens.
 *
 * SECRET KEY must be the SAME in:
 *   - customer-service (generates token)
 *   - api-gateway (validates token)
 *
 * In production: load from Azure Key Vault
 * In demo: hardcoded (never do this in production)
 * ================================================================
 */
@Component
public class JwtUtils {

    // MUST match the secret in api-gateway JwtAuthFilter
    public static final String SECRET =
            "al-hilal-bank-super-secret-key-must-be-256-bits-long!!";

    private static final long EXPIRY_MS = 15 * 60 * 1000; // 15 minutes

    private final SecretKey key = Keys.hmacShaKeyFor(
            SECRET.getBytes(StandardCharsets.UTF_8)
    );

    /**
     * Generate JWT token after successful login
     */
    public String generateToken(String customerId, List<String> roles) {
        return Jwts.builder()
                .subject(customerId)                    // who this token is for
                .claim("roles", roles)                  // what they can do
                .claim("iss", "al-hilal-bank")          // who issued it
                .issuedAt(new Date())                   // when issued
                .expiration(new Date(
                        System.currentTimeMillis() + EXPIRY_MS)) // when expires
                .signWith(key)                          // sign with secret
                .compact();
    }

    /**
     * Validate token and extract all claims
     */
    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}