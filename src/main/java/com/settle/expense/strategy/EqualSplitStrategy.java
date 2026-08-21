package com.settle.expense.strategy;

import com.settle.expense.EqualSplitCalculator;
import com.settle.expense.SplitType;
import com.settle.expense.dto.CreateExpenseRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public SplitType getSupportedType() {
        return SplitType.EQUAL;
    }

    @Override
    public Map<UUID, BigDecimal> calculateSplits(BigDecimal totalAmount, CreateExpenseRequest request) {
        return EqualSplitCalculator.calculateSplits(totalAmount, request.getParticipantUserIds());
    }
}
