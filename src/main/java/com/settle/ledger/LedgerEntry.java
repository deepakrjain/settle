package com.settle.ledger;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * LedgerEntry is an APPEND-ONLY record of a financial obligation between two users.
 *
 * INVARIANT: Rows in this table are NEVER updated or deleted after insertion.
 * This guarantees a complete, tamper-evident audit trail of all financial movements.
 * If an expense is corrected, a compensating ledger entry is inserted — the original
 * is never mutated. The repository interface deliberately omits any update or delete
 * methods to enforce this at the application layer. The database should additionally
 * enforce this with appropriate permissions in production.
 *
 * Each entry records: "fromUserId owes toUserId this amount, because of sourceType/sourceId."
 */
@Entity
@Table(name = "ledger_entries")
@Data
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    /** The user who OWES money (the debtor). */
    @Column(name = "from_user_id", nullable = false)
    private UUID fromUserId;

    /** The user who is OWED money (the creditor / payer). */
    @Column(name = "to_user_id", nullable = false)
    private UUID toUserId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currency;

    /** EXPENSE — generated when an expense is created; SETTLEMENT — generated when a debt is settled. */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    /** The ID of the Expense or Settlement that caused this entry. */
    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
