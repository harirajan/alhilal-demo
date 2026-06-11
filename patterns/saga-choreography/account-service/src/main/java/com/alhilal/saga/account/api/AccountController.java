package com.alhilal.saga.account.api;

import com.alhilal.saga.account.service.AccountSagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/saga/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountSagaService accountSagaService;

    @GetMapping("/balances")
    public ResponseEntity<Map<String, Double>> getBalances() {
        return ResponseEntity.ok(accountSagaService.getAllBalances());
    }
}
