package com.settle.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettlementTransaction {
    private UUID fromUserId;
    private UUID toUserId;
    private BigDecimal amount;
}
