package com.digitalWallet.payment_service.dtos.requests;

import java.math.BigDecimal;

public record InternalTransactionRequest(
        BigDecimal amount,
        String description,
        String referenceId
) {
}
