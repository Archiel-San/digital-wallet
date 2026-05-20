package com.digitalWallet.payment_service.dtos.responses;

import java.math.BigDecimal;

public record WalletResponse(
        Long id,
        String keycloakId,
        String status,
        BigDecimal amount
) {
}
