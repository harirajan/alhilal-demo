package com.alhilal.account;

import com.alhilal.account.domain.Account;
import com.alhilal.account.domain.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import com.alhilal.account.domain.Account;
import com.alhilal.account.domain.AccountService;  // ← add this line
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@SpringBootApplication
public class AccountServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
        System.out.println("""
            
            ╔══════════════════════════════════════════════════╗
            ║   account-services running ✓                     ║
            ║   Swagger: http://localhost:8083/swagger-ui.html ║
            ║   H2 Console: http://localhost:8083/h2-console   ║
            ╚══════════════════════════════════════════════════╝
            """);
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
class AccountDataSeeder implements CommandLineRunner {

    private final AccountService accountService;

    @Override
    public void run(String... args) {
        try {
            // NOTE: We use a hardcoded customerId here for demo purposes.
            // In real system: account-service would have received a
            // CustomerVerifiedEvent from Kafka and stored the customerId.
            // For local demo, customer-service seeds CUST-* IDs — copy one here.
            // We use a placeholder — you'll replace with actual ID from customer-service logs.

            String demoCustomerId = "CUST-DEMO001";

            Account savings = accountService.openAccount(demoCustomerId, Account.AccountType.SAVINGS, "AED");
            accountService.deposit(savings.getAccountId(), new BigDecimal("50000"), "Initial deposit");

            Account murabaha = accountService.openAccount(demoCustomerId, Account.AccountType.MURABAHA, "AED");
            accountService.deposit(murabaha.getAccountId(), new BigDecimal("5000"), "Murabaha payment");

            log.info("=== Demo accounts created ===");
            log.info("Savings account:  {}", savings.getAccountId());
            log.info("Murabaha account: {}", murabaha.getAccountId());
            log.info("Use these IDs in BFF calls");

        } catch (Exception e) {
            log.warn("Account seeding skipped: {}", e.getMessage());
        }
    }
}
