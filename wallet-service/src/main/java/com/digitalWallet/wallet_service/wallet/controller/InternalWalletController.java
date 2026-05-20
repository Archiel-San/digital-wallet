package com.digitalWallet.wallet_service.wallet.controller;

import com.digitalWallet.wallet_service.wallet.dtos.InternalTransactionRequest;
import com.digitalWallet.wallet_service.wallet.dtos.WalletResponse;
import com.digitalWallet.wallet_service.wallet.repository.WalletRepository;
import com.digitalWallet.wallet_service.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/wallets")
@RequiredArgsConstructor
public class InternalWalletController {

    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletService walletService;

    @GetMapping("/by-keycloak/{keycloakId}")
    public ResponseEntity<WalletResponse> getByKeycloakId(
            @PathVariable String keycloakId) {
        return ResponseEntity.ok(walletService.getOrCreateWallet(keycloakId));
    }

    @PostMapping("/{walletId}/debit")
    public ResponseEntity<Void> debit(
            @PathVariable Long walletId,
            @RequestBody InternalTransactionRequest request) {
        walletService.debit(walletId, request.amount(),
                request.description(), request.referenceId());
        return ResponseEntity.ok().build();
    }


    @PostMapping("/{walletId}/credit")
    public ResponseEntity<Void> credit(
            @PathVariable Long walletId,
            @RequestBody InternalTransactionRequest request) {
        walletService.credit(walletId, request.amount(),
                request.description(), request.referenceId());
        return ResponseEntity.ok().build();
    }
}
