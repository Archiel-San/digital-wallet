package com.digitalWallet.payment_service.repository;

import com.digitalWallet.payment_service.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findBySenderKeycloakIdOrReceiverKeycloakIdOrderByCreatedAtDesc(
            String senderKeycloakId, String receiverKeycloakId);

}
