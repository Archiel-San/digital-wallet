package com.digitalWallet.user_service.dtos.requests;

import jakarta.annotation.Nonnull;

public record LoginRequest(
        @Nonnull String email,
        @Nonnull String password
) {
}
