package com.digitalWallet.user_service.dtos.responses;

import com.digitalWallet.user_service.domain.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String keycloakId,
        String email,
        String firstName,
        String lastName,
        String phone,
        LocalDateTime createdAt

) {
    public UserResponse(User user){
        this(user.getId(), user.getKeycloakId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhone(), user.getCreatedAt());
    }
}
