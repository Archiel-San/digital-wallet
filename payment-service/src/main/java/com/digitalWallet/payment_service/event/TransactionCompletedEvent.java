package com.digitalWallet.payment_service.event;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record TransactionCompletedEvent(
        Long transactionId,
        String senderKeycloakId,
        String receiverKeycloakId,
        BigDecimal amount,
        String description,
        LocalDateTime completedAt
) {
}
