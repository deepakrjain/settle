package com.settle.expense.strategy;

import com.settle.expense.SplitType;
import com.settle.expense.dto.CreateExpenseRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class ExactSplitStrategy implements SplitStrategy {

    @Override
    public SplitType getSupportedType() {
        return SplitType.EXACT;
    }

    @Override
    public Map<UUID, BigDecimal> calculateSplits(BigDecimal totalAmount, CreateExpenseRequest request) {
        Map<UUID, BigDecimal> exactAmounts = request.getExactAmounts();
        if (exactAmounts == null || exactAmounts.isEmpty()) {
            throw new IllegalArgumentException("Exact amounts map cannot be empty");
        }

        BigDecimal sum = exactAmounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException("Sum of exact amounts (" + sum + ") does not equal total amount (" + totalAmount + ")");
        }

        return exactAmounts;
    }
}
