package com.settle.expense.strategy;

import com.settle.expense.EqualSplitCalculator;
import com.settle.expense.SplitType;
import com.settle.expense.dto.CreateExpenseRequest;
import com.settle.expense.dto.ItemRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class ItemizedSplitStrategy implements SplitStrategy {

    @Override
    public SplitType getSupportedType() {
        return SplitType.ITEMIZED;
    }

    @Override
    public Map<UUID, BigDecimal> calculateSplits(BigDecimal totalAmount, CreateExpenseRequest request) {
        List<ItemRequest> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be empty for itemized split");
        }

        BigDecimal taxAndTip = request.getTaxAndTip() != null ? request.getTaxAndTip() : BigDecimal.ZERO;

        // Step 1: Accumulate each user's share of individual items
        Map<UUID, BigDecimal> userItemSubtotals = new HashMap<>();

        for (ItemRequest item : items) {
            Map<UUID, BigDecimal> itemSplits = EqualSplitCalculator.calculateSplits(item.getAmount(), item.getParticipantUserIds());
            for (Map.Entry<UUID, BigDecimal> entry : itemSplits.entrySet()) {
                userItemSubtotals.merge(entry.getKey(), entry.getValue(), BigDecimal::add);
            }
        }

        BigDecimal totalItemSubtotal = userItemSubtotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 2: Validate total amount matches item subtotal + tax/tip if total amount is provided
        BigDecimal expectedTotal = totalItemSubtotal.add(taxAndTip);
        if (totalAmount != null && totalAmount.compareTo(expectedTotal) != 0) {
            throw new IllegalArgumentException("Total amount (" + totalAmount + ") does not equal items subtotal + tax/tip (" + expectedTotal + ")");
        }

        List<UUID> sortedUsers = new ArrayList<>(userItemSubtotals.keySet());
        Collections.sort(sortedUsers);

        // Step 3: Distribute tax & tip proportionally across each user's item subtotal
        Map<UUID, BigDecimal> finalSplits = new LinkedHashMap<>();

        if (taxAndTip.compareTo(BigDecimal.ZERO) > 0 && totalItemSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            Map<UUID, BigDecimal> baseTaxTipShares = new HashMap<>();
            BigDecimal totalTaxTipAllocated = BigDecimal.ZERO;

            for (UUID userId : sortedUsers) {
                BigDecimal userSubtotal = userItemSubtotals.get(userId);
                // userTaxTipShare = taxAndTip * (userSubtotal / totalItemSubtotal)
                BigDecimal taxTipShare = taxAndTip.multiply(userSubtotal)
                        .divide(totalItemSubtotal, 2, RoundingMode.DOWN);

                baseTaxTipShares.put(userId, taxTipShare);
                totalTaxTipAllocated = totalTaxTipAllocated.add(taxTipShare);
            }

            BigDecimal remainder = taxAndTip.subtract(totalTaxTipAllocated);
            int extraUnits = remainder.multiply(new BigDecimal("100")).intValueExact();

            for (int i = 0; i < sortedUsers.size(); i++) {
                UUID userId = sortedUsers.get(i);
                BigDecimal subtotal = userItemSubtotals.get(userId);
                BigDecimal taxTipShare = baseTaxTipShares.get(userId);
                if (i < extraUnits) {
                    taxTipShare = taxTipShare.add(new BigDecimal("0.01"));
                }
                finalSplits.put(userId, subtotal.add(taxTipShare));
            }
        } else {
            for (UUID userId : sortedUsers) {
                finalSplits.put(userId, userItemSubtotals.get(userId));
            }
        }

        return finalSplits;
    }
}
