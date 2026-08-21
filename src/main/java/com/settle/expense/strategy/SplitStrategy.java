package com.settle.expense.strategy;

import com.settle.expense.SplitType;
import com.settle.expense.dto.CreateExpenseRequest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface SplitStrategy {
    SplitType getSupportedType();
    Map<UUID, BigDecimal> calculateSplits(BigDecimal totalAmount, CreateExpenseRequest request);
}
