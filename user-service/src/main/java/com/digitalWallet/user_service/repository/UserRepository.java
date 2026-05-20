package com.digitalWallet.user_service.repository;

import com.digitalWallet.user_service.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBykeycloakId(String keycloakId);
    Boolean existsByEmail(String email);


}
