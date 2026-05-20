package com.digitalWallet.user_service.dtos.requests;

import jakarta.validation.constraints.NotBlank;

// dto/RefreshTokenRequest.java
public record RefreshTokenRequest(
        @NotBlank String refreshToken) {}
