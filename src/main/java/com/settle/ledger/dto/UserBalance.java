package com.settle.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBalance {
    private UUID userId;
    /** Positive = this user is owed money (net creditor). Negative = this user owes money (net debtor). */
    private BigDecimal netBalance;
}
