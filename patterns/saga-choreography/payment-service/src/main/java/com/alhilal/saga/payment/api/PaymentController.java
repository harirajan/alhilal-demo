package com.alhilal.saga.payment.api;

import com.alhilal.saga.payment.model.Payment;
import com.alhilal.saga.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/saga/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/transfer")
    public ResponseEntity<Payment> transfer(
            @RequestBody TransferRequest request) throws Exception {

        Payment payment = paymentService.initiatePayment(
            request.fromAccount(),
            request.toAccount(),
            request.amount(),
            request.customerId()
        );

        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{sagaId}")
    public ResponseEntity<Payment> getPayment(
            @PathVariable String sagaId) {

        Payment payment = paymentService.getPayment(sagaId);
        if (payment == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping
    public ResponseEntity<Map<String, Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    public record TransferRequest(
        String fromAccount,
        String toAccount,
        Double amount,
        String customerId
    ) {}
}
