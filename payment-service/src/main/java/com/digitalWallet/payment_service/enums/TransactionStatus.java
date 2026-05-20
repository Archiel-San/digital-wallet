package com.digitalWallet.payment_service.enums;

public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    COMPENSATED  // saga rolled back
}
