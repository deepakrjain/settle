package com.settle.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettlementPlanResponse {
    private List<SettlementTransaction> greedyPlan;
    private int greedyTransactionCount;
    private List<SettlementTransaction> optimalPlan;
    private Integer optimalTransactionCount;
    private boolean optimalCalculated;
}
