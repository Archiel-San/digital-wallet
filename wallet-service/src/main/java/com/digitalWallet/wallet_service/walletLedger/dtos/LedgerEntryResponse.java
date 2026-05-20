package com.digitalWallet.wallet_service.walletLedger.dtos;

import com.digitalWallet.wallet_service.walletLedger.domain.WalletLedger;
import com.digitalWallet.wallet_service.walletLedger.enums.LedgerType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LedgerEntryResponse(
        Long id,
        LedgerType ledgerType,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description, LocalDateTime createdAt
) {

    public LedgerEntryResponse(WalletLedger walletLedger){
        this(walletLedger.getId(), walletLedger.getLedgerType(),walletLedger.getAmount(),
                walletLedger.getBalanceAfter(), walletLedger.getDescription(), walletLedger.getCreatedAt());
    }

}
