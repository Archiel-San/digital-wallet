package com.digitalWallet.payment_service.service;

import com.digitalWallet.payment_service.domain.Transaction;
import com.digitalWallet.payment_service.dtos.requests.TransferRequest;
import com.digitalWallet.payment_service.dtos.responses.TransactionResponse;
import com.digitalWallet.payment_service.dtos.responses.WalletResponse;
import com.digitalWallet.payment_service.enums.TransactionStatus;
import com.digitalWallet.payment_service.event.TransactionCompletedEvent;
import com.digitalWallet.payment_service.event.TransactionFailedEvent;
import com.digitalWallet.payment_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final WalletClient walletClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;


    public TransactionResponse transfer(String senderKeycloakId, TransferRequest request) {

        if (senderKeycloakId.equals(request.receiverKeycloakId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot transfer to yourself");
        }

        // ── Resolve wallets ───────────────────────────────────────────────────
        WalletResponse senderWallet = walletClient.getWalletByKeycloakId(senderKeycloakId);
        WalletResponse receiverWallet = walletClient.getWalletByKeycloakId(
                request.receiverKeycloakId());

        if (senderWallet == null || receiverWallet == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found");
        }

        // ── Create transaction record (PENDING) ───────────────────────────────
        Transaction transaction = Transaction.builder()
                .senderKeycloakId(senderKeycloakId)
                .receiverKeycloakId(request.receiverKeycloakId())
                .senderWalletId(senderWallet.id())
                .receiverWalletId(receiverWallet.id())
                .amount(request.amount())
                .description(request.description())
                .status(TransactionStatus.PENDING)
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Transaction {} created PENDING", transaction.getId());

        // ── SAGA Step 1: Debit sender ─────────────────────────────────────────
        try {
            walletClient.debit(
                    senderWallet.id(),
                    request.amount(),
                    "Transfer to " + request.receiverKeycloakId(),
                    "DEBIT-" + transaction.getId()  // referenceId for idempotency
            );
            log.info("Transaction {} — sender debited", transaction.getId());
        } catch (Exception e) {
            // Debit failed — no money moved, just mark as FAILED
            log.error("Transaction {} — debit failed: {}", transaction.getId(), e.getMessage());
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Debit failed: " + e.getMessage());
            transactionRepository.save(transaction);

            kafkaTemplate.send("transaction.failed", TransactionFailedEvent.builder()
                    .transactionId(transaction.getId())
                    .senderKeycloakId(senderKeycloakId)
                    .reason(e.getMessage())
                    .failedAt(LocalDateTime.now())
                    .build());

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transfer failed: " + e.getMessage());
        }

        // ── SAGA Step 2: Credit receiver ──────────────────────────────────────
        try {
            walletClient.credit(
                    receiverWallet.id(),
                    request.amount(),
                    "Transfer from " + senderKeycloakId,
                    "CREDIT-" + transaction.getId()  // referenceId for idempotency
            );
            log.info("Transaction {} — receiver credited", transaction.getId());
        } catch (Exception e) {
            // Credit failed — COMPENSATE: refund sender
            log.error("Transaction {} — credit failed, compensating...", transaction.getId());

            try {
                walletClient.credit(
                        senderWallet.id(),
                        request.amount(),
                        "Refund: transfer failed",
                        "COMPENSATE-" + transaction.getId()  // different referenceId
                );
                transaction.setStatus(TransactionStatus.COMPENSATED);
                log.info("Transaction {} — compensation successful", transaction.getId());
            } catch (Exception compensationEx) {
                // Compensation also failed — needs manual intervention
                // In production: dead letter queue, alert ops team
                log.error("Transaction {} — COMPENSATION FAILED: {}",
                        transaction.getId(), compensationEx.getMessage());
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason("CRITICAL: compensation failed — " +
                        compensationEx.getMessage());
            }

            transactionRepository.save(transaction);

            kafkaTemplate.send("transaction.failed", TransactionFailedEvent.builder()
                    .transactionId(transaction.getId())
                    .senderKeycloakId(senderKeycloakId)
                    .reason("Credit failed, compensation attempted")
                    .failedAt(LocalDateTime.now())
                    .build());

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Transfer failed — funds have been returned");
        }

        // ── SAGA Complete: mark COMPLETED and emit event ───────────────────────
        transaction.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(transaction);

        kafkaTemplate.send("transaction.completed", TransactionCompletedEvent.builder()
                .transactionId(transaction.getId())
                .senderKeycloakId(senderKeycloakId)
                .receiverKeycloakId(request.receiverKeycloakId())
                .amount(request.amount())
                .description(request.description())
                .completedAt(LocalDateTime.now())
                .build());

        log.info("Transaction {} COMPLETED", transaction.getId());
        return new TransactionResponse(transaction);
    }



    // ── Get transaction history for current user ──────────────────────────────
    @Transactional(readOnly = true)
    public List<TransactionResponse> getHistory(String keycloakId) {
        return transactionRepository
                .findBySenderKeycloakIdOrReceiverKeycloakIdOrderByCreatedAtDesc(
                        keycloakId, keycloakId)
                .stream()
                .map(TransactionResponse::new)
                .toList();
    }

}
