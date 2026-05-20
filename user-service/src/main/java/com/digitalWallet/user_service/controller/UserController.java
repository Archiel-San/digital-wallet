package com.digitalWallet.user_service.controller;

import com.digitalWallet.user_service.domain.User;
import com.digitalWallet.user_service.dtos.responses.UserResponse;
import com.digitalWallet.user_service.service.UserSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private UserSyncService userSyncService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication
    ){
        Jwt resolvedJwt = resolveJwt(jwt, authentication);
        User user = userSyncService.syncFromJwt(resolvedJwt);
        return ResponseEntity.ok(new UserResponse(user));
    }

    private Jwt resolveJwt(Jwt jwt, Authentication authentication) {
        if (jwt != null) {
            return jwt;
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing JWT principal");
    }
}
