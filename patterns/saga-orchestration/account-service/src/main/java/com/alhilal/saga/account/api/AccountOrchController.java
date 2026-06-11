package com.alhilal.saga.account.api;

import com.alhilal.saga.account.service.AccountOrchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orch/accounts")
@RequiredArgsConstructor
public class AccountOrchController {
    private final AccountOrchService service;

    @GetMapping("/balances")
    public ResponseEntity<Map<String, Double>> balances() {
        return ResponseEntity.ok(service.getBalances());
    }
}
