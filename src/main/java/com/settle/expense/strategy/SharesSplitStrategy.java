package com.settle.expense.strategy;

import com.settle.expense.SplitType;
import com.settle.expense.dto.CreateExpenseRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class SharesSplitStrategy implements SplitStrategy {

    @Override
    public SplitType getSupportedType() {
        return SplitType.SHARES;
    }

    @Override
    public Map<UUID, BigDecimal> calculateSplits(BigDecimal totalAmount, CreateExpenseRequest request) {
        Map<UUID, Integer> shares = request.getShares();
        if (shares == null || shares.isEmpty()) {
            throw new IllegalArgumentException("Shares map cannot be empty");
        }

        int totalShares = shares.values().stream().mapToInt(Integer::intValue).sum();
        if (totalShares <= 0) {
            throw new IllegalArgumentException("Total shares must be greater than zero");
        }

        List<UUID> sortedUsers = new ArrayList<>(shares.keySet());
        Collections.sort(sortedUsers);

        Map<UUID, BigDecimal> baseShares = new LinkedHashMap<>();
        BigDecimal totalAllocated = BigDecimal.ZERO;

        for (UUID userId : sortedUsers) {
            int userShareCount = shares.get(userId);
            // share = totalAmount * (userShareCount / totalShares)
            BigDecimal share = totalAmount.multiply(BigDecimal.valueOf(userShareCount))
                    .divide(BigDecimal.valueOf(totalShares), 2, RoundingMode.DOWN);

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
