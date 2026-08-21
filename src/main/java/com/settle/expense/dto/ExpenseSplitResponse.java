package com.settle.expense.dto;

import com.settle.expense.ExpenseSplit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseSplitResponse {
    private UUID id;
    private UUID userId;
    private BigDecimal shareAmount;

    public static ExpenseSplitResponse fromEntity(ExpenseSplit split) {
        return new ExpenseSplitResponse(
            split.getId(),
            split.getUserId(),
            split.getShareAmount()
        );
    }
}
