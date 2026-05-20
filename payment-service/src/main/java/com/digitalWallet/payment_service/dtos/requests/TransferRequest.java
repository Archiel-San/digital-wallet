package com.digitalWallet.payment_service.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String receiverKeycloakId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String description
) {
}
