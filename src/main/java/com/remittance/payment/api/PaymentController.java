package com.remittance.payment.api;

import com.remittance.payment.api.dto.PaymentResponse;
import com.remittance.payment.application.PaymentService;
import com.remittance.payment.domain.Payment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        Payment payment = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @GetMapping("/remittance/{remittanceId}")
    public ResponseEntity<PaymentResponse> getPaymentByRemittanceId(
            @PathVariable UUID remittanceId) {
        Payment payment = paymentService.getPaymentByRemittanceId(remittanceId);
        return ResponseEntity.ok(PaymentResponse.from(payment));
    }
}
