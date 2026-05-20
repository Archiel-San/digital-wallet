package com.digitalWallet.wallet_service.wallet.controller;

import com.digitalWallet.wallet_service.wallet.dtos.DepositRequest;
import com.digitalWallet.wallet_service.wallet.dtos.WalletResponse;
import com.digitalWallet.wallet_service.wallet.service.WalletService;
import com.digitalWallet.wallet_service.walletLedger.dtos.LedgerEntryResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/// Class for tests
@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    @Autowired
    private WalletService walletService;

    // Lazy init — creates wallet on first hit
    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(@AuthenticationPrincipal Jwt jwt){
        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(walletService.getOrCreateWallet(keycloakId));
    }


    // Full ledger history
    @GetMapping("/me/ledger")
    public ResponseEntity<List<LedgerEntryResponse>> getMyLedger(
            @AuthenticationPrincipal Jwt jwt) {

        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(walletService.getLedger(keycloakId));
    }

    // Deposit — for testing only, production deposits come from Payment Service
    @PostMapping("/me/deposit")
    public ResponseEntity<WalletResponse> deposit(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid DepositRequest request) {

        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(walletService.deposit(keycloakId, request));
    }




}
