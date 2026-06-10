package com.alhilal.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ================================================================
 * API GATEWAY — local version of Kong
 * ================================================================
 * In Al Hilal production: Kong API Gateway
 * Here: Spring Cloud Gateway (same concepts, easier to run locally)
 *
 * What it does:
 *   - Single entry point for ALL requests (port 8080)
 *   - Routes /bff/** → bff-retail-service (port 8081)
 *   - Routes /api/v1/customers/** → customer-service (port 8082)
 *   - Routes /api/v1/accounts/** → account-service (port 8083)
 *   - Adds CORS headers
 *   - Logs all requests
 *
 * Mobile app only needs to know ONE URL: http://localhost:8080
 * It never calls 8081, 8082, 8083 directly.
 * ================================================================
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
        System.out.println("""
            
            ╔══════════════════════════════════════════════════════╗
            ║   API Gateway running ✓  (local Kong equivalent)    ║
            ║   Port: 8080 — use THIS port for all requests        ║
            ╠══════════════════════════════════════════════════════╣
            ║   Routes:                                            ║
            ║   /bff/**           → bff-retail-service (:8081)    ║
            ║   /api/v1/customers → customer-service   (:8082)    ║
            ║   /api/v1/accounts  → account-service    (:8083)    ║
            ╠══════════════════════════════════════════════════════╣
            ║   Try: GET http://localhost:8080/bff/v1/home/{id}   ║
            ╚══════════════════════════════════════════════════════╝
            """);
    }
}
