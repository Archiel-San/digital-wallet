package com.digitalWallet.user_service.service;

import com.digitalWallet.user_service.domain.User;
import com.digitalWallet.user_service.dtos.responses.UserResponse;
import com.digitalWallet.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserSyncService {

    @Autowired
    private UserRepository userRepository;

    /// Criacao da mensagem para criacao da carteira do usuario
    private KafkaTemplate<String, UserResponse> kafkaTemplate;

    //called on every authenticated request to /api/users/me
    // If the user doesn't exist in our DB yet, create them from the JWT claims
    public User syncFromJwt(Jwt jwt){
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing JWT principal");
        }

        String keycloakId = jwt.getSubject();

        return userRepository.findBykeycloakId(keycloakId).orElseGet(
                ()-> {
                    log.info("First Login for KeycloakId={}, syncing to DB", keycloakId);
                    return createFromJwt(jwt);
                }
        );
    }

    private User createFromJwt(Jwt jwt){
        User user = new User();
        user.setKeycloakId(jwt.getSubject());
        user.setEmail(jwt.getClaimAsString("email"));
        user.setFirstName(jwt.getClaimAsString("given_name"));
        user.setLastName(jwt.getClaimAsString("family_name"));



        return userRepository.save(user);
    }




}
