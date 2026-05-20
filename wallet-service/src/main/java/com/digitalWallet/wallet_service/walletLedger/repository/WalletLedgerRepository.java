package com.digitalWallet.wallet_service.walletLedger.repository;

import com.digitalWallet.wallet_service.wallet.domain.Wallet;
import com.digitalWallet.wallet_service.walletLedger.domain.WalletLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletLedgerRepository extends JpaRepository<WalletLedger, Long> {
    Optional<WalletLedger> findTopByWalletOrderByCreatedAtDesc(Wallet wallet);

    // Full history for a wallet
    List<WalletLedger> findByWalletOrderByCreatedAtDesc(Wallet wallet);

    // Idempotency check
    boolean existsByReferenceId(String referenceId);

}
