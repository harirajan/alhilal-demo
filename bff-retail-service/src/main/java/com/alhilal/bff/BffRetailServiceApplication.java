package com.alhilal.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ================================================================
 * BFF RETAIL SERVICE
 * ================================================================
 * @EnableFeignClients — activates the Feign HTTP clients
 * Without this, the @FeignClient interfaces won't work
 * ================================================================
 */
@SpringBootApplication
@EnableFeignClients
public class BffRetailServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BffRetailServiceApplication.class, args);
        System.out.println("""
            
            ╔══════════════════════════════════════════════════╗
            ║   bff-retail-services running ✓                  ║
            ║   Swagger: http://localhost:8081/swagger-ui.html ║
            ╠══════════════════════════════════════════════════╣
            ║   KEY ENDPOINT TO TRY:                           ║
            ║   GET /bff/v1/home/{customerId}                  ║
            ║   Watch logs — see parallel calls to both        ║
            ║   customer-service and account-service           ║
            ╚══════════════════════════════════════════════════╝
            """);
    }
}
