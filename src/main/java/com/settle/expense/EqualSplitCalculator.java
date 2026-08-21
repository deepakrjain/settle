package com.settle.expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class EqualSplitCalculator {

    /**
     * Calculates equal splits for a given total amount among participants.
     * Remainder handling rule:
     * When splitting an amount (e.g. ₹100.00 among 3 participants = 33.3333...),
     * base shares are rounded DOWN to 2 decimal places (₹33.33 * 3 = ₹99.99).
     * The remaining 1 paisa (₹0.01) is distributed deterministically to participants
     * sorted by their UUID in ascending natural order.
     * This guarantees that:
     * 1. Sum of shares ALWAYS equals the exact total amount.
     * 2. The distribution is completely deterministic and reproducible.
     */
    public static Map<UUID, BigDecimal> calculateSplits(BigDecimal totalAmount, Set<UUID> participants) {
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("Participants set cannot be empty");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }

        // Sort participant IDs deterministically so remainder allocation is stable and reproducible
        List<UUID> sortedParticipants = new ArrayList<>(participants);
        Collections.sort(sortedParticipants);

        int count = sortedParticipants.size();
        BigDecimal baseShare = totalAmount.divide(BigDecimal.valueOf(count), 2, RoundingMode.DOWN);
        BigDecimal totalBaseAllocated = baseShare.multiply(BigDecimal.valueOf(count));
        BigDecimal remainder = totalAmount.subtract(totalBaseAllocated);

        // Convert remainder to number of 0.01 units (cents/paisa)
        int extraUnits = remainder.multiply(BigDecimal.valueOf(100)).intValueExact();

        Map<UUID, BigDecimal> splits = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            BigDecimal userShare = baseShare;
            if (i < extraUnits) {
                userShare = userShare.add(new BigDecimal("0.01"));
            }
            splits.put(sortedParticipants.get(i), userShare);
        }

        return splits;
    }
}
