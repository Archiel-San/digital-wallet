package com.digitalWallet.user_service.dtos.responses;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long expiresIn
) {
}
