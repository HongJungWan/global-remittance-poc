package com.remittance.remittance.api;

import com.remittance.remittance.api.dto.ConfirmQuoteRequest;
import com.remittance.remittance.api.dto.CreateQuoteRequest;
import com.remittance.remittance.api.dto.RemittanceOrderResponse;
import com.remittance.remittance.application.RemittanceService;
import com.remittance.remittance.domain.RemittanceOrder;
import com.remittance.remittance.domain.vo.ReceiverInfo;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/remittance")
public class RemittanceController {

    private final RemittanceService remittanceService;

    public RemittanceController(RemittanceService remittanceService) {
        this.remittanceService = remittanceService;
    }

    @PostMapping("/quote")
    public ResponseEntity<RemittanceOrderResponse> createQuote(
            @AuthenticationPrincipal UUID senderId,
            @Valid @RequestBody CreateQuoteRequest request) {
        ReceiverInfo receiverInfo = new ReceiverInfo(
                request.receiverName(), request.receiverAccount(),
                request.receiverBankCode(), request.receiverCountry());

        RemittanceOrder order = remittanceService.createQuote(
                senderId, receiverInfo,
                request.sourceCurrency(), request.targetCurrency(),
                request.sourceAmount(), request.paymentMethod());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RemittanceOrderResponse.from(order));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<RemittanceOrderResponse> confirmQuote(
            @PathVariable UUID orderId,
            @Valid @RequestBody ConfirmQuoteRequest request) {
        RemittanceOrder order = remittanceService.confirmQuote(orderId, request.paymentMethod());
        return ResponseEntity.ok(RemittanceOrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<RemittanceOrderResponse> getOrder(@PathVariable UUID orderId) {
        RemittanceOrder order = remittanceService.getOrder(orderId);
        return ResponseEntity.ok(RemittanceOrderResponse.from(order));
    }

    @GetMapping
    public ResponseEntity<List<RemittanceOrderResponse>> getMyOrders(
            @AuthenticationPrincipal UUID senderId) {
        List<RemittanceOrderResponse> orders = remittanceService.getOrdersBySender(senderId)
                .stream().map(RemittanceOrderResponse::from).toList();
        return ResponseEntity.ok(orders);
    }
}
