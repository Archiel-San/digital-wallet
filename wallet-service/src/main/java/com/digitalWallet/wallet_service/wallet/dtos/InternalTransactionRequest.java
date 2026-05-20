package com.digitalWallet.wallet_service.wallet.dtos;

import java.math.BigDecimal;

// dto/InternalTransactionRequest.java  (in wallet-service)
public record InternalTransactionRequest(
        BigDecimal amount,
        String description,
        String referenceId
) {}
