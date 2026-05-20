package com.digitalWallet.wallet_service.wallet.dtos;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record DepositRequest(
        @Nonnull
        @DecimalMin("0.01")
        BigDecimal amount,
        String description

) {
}
