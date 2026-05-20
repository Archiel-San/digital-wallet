package com.digitalWallet.wallet_service.wallet.repository;

import com.digitalWallet.wallet_service.wallet.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface   WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByKeycloakId(String keycloakId);

}
