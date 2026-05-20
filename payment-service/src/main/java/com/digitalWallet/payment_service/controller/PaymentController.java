package com.digitalWallet.payment_service.controller;

import com.digitalWallet.payment_service.dtos.requests.TransferRequest;
import com.digitalWallet.payment_service.dtos.responses.TransactionResponse;
import com.digitalWallet.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;


    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid TransferRequest request) {

        String senderKeycloakId = jwt.getSubject();
        return ResponseEntity.ok(paymentService.transfer(senderKeycloakId, request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<TransactionResponse>> getHistory(
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(paymentService.getHistory(keycloakId));
    }


}
