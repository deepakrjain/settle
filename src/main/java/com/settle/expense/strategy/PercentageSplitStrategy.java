package com.settle.expense.strategy;

import com.settle.expense.SplitType;
import com.settle.expense.dto.CreateExpenseRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class PercentageSplitStrategy implements SplitStrategy {

    @Override
    public SplitType getSupportedType() {
        return SplitType.PERCENTAGE;
    }

    @Override
    public Map<UUID, BigDecimal> calculateSplits(BigDecimal totalAmount, CreateExpenseRequest request) {
        Map<UUID, BigDecimal> percentages = request.getPercentages();
        if (percentages == null || percentages.isEmpty()) {
            throw new IllegalArgumentException("Percentages map cannot be empty");
        }

        BigDecimal totalPercentage = percentages.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPercentage.compareTo(new BigDecimal("100.00")) != 0 && totalPercentage.compareTo(new BigDecimal("100")) != 0) {
            throw new IllegalArgumentException("Total percentage must equal 100% (got " + totalPercentage + "%)");
        }

        List<UUID> sortedUsers = new ArrayList<>(percentages.keySet());
        Collections.sort(sortedUsers);

        Map<UUID, BigDecimal> baseShares = new LinkedHashMap<>();
        BigDecimal totalAllocated = BigDecimal.ZERO;

        for (UUID userId : sortedUsers) {
            BigDecimal pct = percentages.get(userId);
            // share = totalAmount * (pct / 100)
            BigDecimal rawShare = totalAmount.multiply(pct).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal share = rawShare.setScale(2, RoundingMode.DOWN);
            baseShares.put(userId, share);
            totalAllocated = totalAllocated.add(share);
        }

        BigDecimal remainder = totalAmount.subtract(totalAllocated);
        int extraUnits = remainder.multiply(new BigDecimal("100")).intValueExact();

        Map<UUID, BigDecimal> finalSplits = new LinkedHashMap<>();
        for (int i = 0; i < sortedUsers.size(); i++) {
            UUID userId = sortedUsers.get(i);
            BigDecimal share = baseShares.get(userId);
            if (i < extraUnits) {
                share = share.add(new BigDecimal("0.01"));
            }
            finalSplits.put(userId, share);
        }

        return finalSplits;
    }
}
