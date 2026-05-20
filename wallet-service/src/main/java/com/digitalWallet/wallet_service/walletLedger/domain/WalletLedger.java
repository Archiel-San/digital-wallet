package com.digitalWallet.wallet_service.walletLedger.domain;

import com.digitalWallet.wallet_service.wallet.domain.Wallet;
import com.digitalWallet.wallet_service.walletLedger.enums.LedgerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "wallet_ledger")
@Builder
public class WalletLedger {

    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    //quando pesquiso 5p transacoes e removo este lazy, este me tras 50 transacoes
    // e a info toda de cada uma da carteira associada se optar pelo lazy,
    // ele ira buscar apenas se fizer transacaox.wallet.getName()
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerType ledgerType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    private String description;

    @Column (unique = true)
    private String referenceId; // idempotency key — prevents duplicate entries

    @CreationTimestamp
    private LocalDateTime createdAt;


}
