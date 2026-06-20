package com.digitalWallet.wallet_service.wallet.service;

import com.digitalWallet.wallet_service.wallet.domain.Wallet;
import com.digitalWallet.wallet_service.wallet.dtos.DepositRequest;
import com.digitalWallet.wallet_service.wallet.dtos.WalletResponse;
import com.digitalWallet.wallet_service.wallet.enums.WalletStatus;
import com.digitalWallet.wallet_service.wallet.repository.WalletRepository;
import com.digitalWallet.wallet_service.walletLedger.domain.WalletLedger;
import com.digitalWallet.wallet_service.walletLedger.dtos.LedgerEntryResponse;
import com.digitalWallet.wallet_service.walletLedger.enums.LedgerType;
import com.digitalWallet.wallet_service.walletLedger.repository.WalletLedgerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class WalletService {

    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private WalletLedgerRepository walletLedgerRepository;

    public WalletResponse getOrCreateWallet(String keycloakId){
        Wallet wallet = walletRepository.findByKeycloakId(keycloakId)
                .orElseGet(()-> {
                    log.info("First access - creating wallet for keycloakId = {}", keycloakId);
                    return createWallet(keycloakId);
                });

        BigDecimal balance = getCurrentBalance(wallet);
        return new WalletResponse(wallet, balance);
    }

    /// Buscar Historico de Transacoes
    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getLedger(String keycloakId){
        Wallet wallet = findWalletByKeycloakId(keycloakId);
        return walletLedgerRepository.findByWalletOrderByCreatedAtDesc(wallet)
                .stream()
                .map(LedgerEntryResponse::new)
                .toList();

    }

    /// ── Deposit (for testing — in production this comes from Payment Service) ─
    public WalletResponse deposit(String keycloakId, DepositRequest request){
        Wallet wallet = findWalletByKeycloakId(keycloakId);
        if (wallet.getStatus() != WalletStatus.ACTIVE){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet is Not Active");
        }

        BigDecimal currentBalance = getCurrentBalance(wallet);
        BigDecimal newBalance =  currentBalance.add((request.amount()));

        /// Idempotency key — prevents duplicate deposits if request is retried
        String referenceId = "DEPOSIT-" + keycloakId + "-" + System.currentTimeMillis();


        WalletLedger entry = new WalletLedger();
        entry.setWallet(wallet);
        entry.setLedgerType(LedgerType.CREDIT);
        entry.setAmount(request.amount());
        entry.setBalanceAfter(newBalance);
        entry.setDescription(request.description() != null? request.description() : "Deposit");
        entry.setReferenceId(referenceId);
        walletLedgerRepository.save(entry);

        log.info("Deposit of {} for keycloakId = {}, new balance = {}", request.amount(), keycloakId, newBalance);
        return new WalletResponse(wallet, newBalance);

    }

    // ── Internal: called by Payment Service via Saga ──────────────────────────
    // These are package-level methods — not exposed via REST
    // Payment Service will call Wallet Service directly (orchestration Saga)
    public void debit(Long walletId, BigDecimal amount, String description, String referenceId){
        //Idempotency - if same referenceId already exists, skip
        if(walletLedgerRepository.existsByReferenceId(referenceId)){
            log.warn("Duplicate debit attempt for referenceId={}, skipping", referenceId);
            return;
        }

        Wallet wallet = walletRepository .findById(walletId).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet Not Found"));
        if(wallet.getStatus() != WalletStatus.ACTIVE){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet is not active");
        }

        BigDecimal currentBalance = getCurrentBalance(wallet);

        if(currentBalance.compareTo(amount)<0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient Funds");
        }

        BigDecimal newBalance = currentBalance.subtract(amount);

        WalletLedger wl = new WalletLedger();
        wl.setWallet(wallet);
        wl.setLedgerType(LedgerType.DEBIT);
        wl.setAmount(amount);
        wl.setBalanceAfter(newBalance);
        wl.setDescription(description);
        wl.setReferenceId(referenceId);
        walletLedgerRepository.save(wl);

        log.info("Debit of {} from walletId={}, new balance={}", amount, walletId, newBalance);
    }

    public void credit(Long walletId, BigDecimal amount,
                       String description, String referenceId) {

        if (walletLedgerRepository.existsByReferenceId(referenceId)) {
            log.warn("Duplicate credit attempt for referenceId={}, skipping", referenceId);
            return;
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Wallet not found"));

        BigDecimal currentBalance = getCurrentBalance(wallet);
        BigDecimal newBalance = currentBalance.add(amount);

        walletLedgerRepository.save(WalletLedger.builder()
                .wallet(wallet)
                .ledgerType(LedgerType.CREDIT)
                .amount(amount)
                .balanceAfter(newBalance)
                .description(description)
                .referenceId(referenceId)
                .build());

        log.info("Credit of {} to walletId={}, new balance={}", amount, walletId, newBalance);
    }


    /// ------------------------Helpers---------------------

    private Wallet findWalletByKeycloakId(String keycloakId) {
        return walletRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Wallet not found"));
    }

    // Nao possuo field de salvar o saldo, mas sim as ultimas transacoes e ve-se o balance after
    private BigDecimal getCurrentBalance(Wallet wallet) {
        return walletLedgerRepository
                .findTopByWalletOrderByCreatedAtDesc(wallet)
                .map(WalletLedger::getBalanceAfter)
                .orElse(BigDecimal.ZERO);
        // no entries yet = zero balance
    }

    private Wallet createWallet(String keycloakId) {
        Wallet wallet = new Wallet();
        wallet.setKeycloakId(keycloakId);
        wallet.setStatus(WalletStatus.ACTIVE);
        return walletRepository.save(wallet);
    }

}
