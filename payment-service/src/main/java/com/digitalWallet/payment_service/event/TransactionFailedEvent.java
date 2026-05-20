package com.digitalWallet.payment_service.event;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TransactionFailedEvent(
        Long transactionId,
        String senderKeycloakId,
        String reason,
        LocalDateTime failedAt
) {
}
