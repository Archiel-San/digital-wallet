package com.digitalWallet.payment_service.dtos.responses;

import com.digitalWallet.payment_service.domain.Transaction;
import com.digitalWallet.payment_service.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        String senderKeycloakId,
        String receiverKeycloakId,
        BigDecimal amount,
        String description,
        TransactionStatus status,
        String failureReason,
        LocalDateTime createdAt
) {
    public TransactionResponse(Transaction tx){
        this(tx.getId(), tx.getSenderKeycloakId(), tx.getReceiverKeycloakId(), tx.getAmount(), tx.getDescription(), tx.getStatus(),tx.getFailureReason(), tx.getCreatedAt());
    }

}
