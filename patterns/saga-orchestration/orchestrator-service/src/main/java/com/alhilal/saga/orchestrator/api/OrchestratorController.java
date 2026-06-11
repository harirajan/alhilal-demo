package com.alhilal.saga.orchestrator.api;

import com.alhilal.saga.orchestrator.model.SagaState;
import com.alhilal.saga.orchestrator.service.SagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orch/payments")
@RequiredArgsConstructor
public class OrchestratorController {

    private final SagaOrchestrator orchestrator;

    @PostMapping("/transfer")
    public ResponseEntity<SagaState> transfer(
            @RequestBody TransferRequest request) throws Exception {
        return ResponseEntity.ok(orchestrator.startSaga(
            request.fromAccount(), request.toAccount(),
            request.amount(), request.customerId()));
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<SagaState> getSaga(
            @PathVariable String sagaId) {
        SagaState saga = orchestrator.getSaga(sagaId);
        return saga != null
            ? ResponseEntity.ok(saga)
            : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<Map<String, SagaState>> all() {
        return ResponseEntity.ok(orchestrator.getAllSagas());
    }

    public record TransferRequest(
        String fromAccount, String toAccount,
        Double amount, String customerId) {}
}
