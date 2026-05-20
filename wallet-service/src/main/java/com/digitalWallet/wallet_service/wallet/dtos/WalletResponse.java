package com.digitalWallet.wallet_service.wallet.dtos;


import com.digitalWallet.wallet_service.wallet.domain.Wallet;
import com.digitalWallet.wallet_service.wallet.enums.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long id,
        String keycloakId,
        WalletStatus walletStatus,
        BigDecimal balance,
        LocalDateTime createdAt
) {

    public WalletResponse(Wallet wallet, BigDecimal balance){
        this(wallet.getId(), wallet.getKeycloakId(), wallet.getStatus(), balance, wallet.getCreatedAt());
    }


}
