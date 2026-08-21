package com.settle.expense;

import com.settle.expense.dto.CreateExpenseRequest;
import com.settle.expense.dto.ItemRequest;
import com.settle.expense.strategy.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SplitStrategyTest {

    private final EqualSplitStrategy equalStrategy = new EqualSplitStrategy();
    private final PercentageSplitStrategy percentageStrategy = new PercentageSplitStrategy();
    private final ExactSplitStrategy exactStrategy = new ExactSplitStrategy();
    private final SharesSplitStrategy sharesStrategy = new SharesSplitStrategy();
    private final ItemizedSplitStrategy itemizedStrategy = new ItemizedSplitStrategy();

    @Test
    @DisplayName("PERCENTAGE split: 50%, 30%, 20% on ₹100.00")
    void testPercentageSplit() {
        UUID u1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID u2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID u3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setSplitType(SplitType.PERCENTAGE);
        req.setPercentages(Map.of(
            u1, new BigDecimal("50.00"),
            u2, new BigDecimal("30.00"),
            u3, new BigDecimal("20.00")
        ));

        Map<UUID, BigDecimal> splits = percentageStrategy.calculateSplits(new BigDecimal("100.00"), req);

        assertEquals(new BigDecimal("50.00"), splits.get(u1));
        assertEquals(new BigDecimal("30.00"), splits.get(u2));
        assertEquals(new BigDecimal("20.00"), splits.get(u3));

        BigDecimal sum = splits.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("100.00"), sum);
    }

    @Test
    @DisplayName("PERCENTAGE split invalid sum throws exception")
    void testPercentageSplitInvalidSum() {
        UUID u1 = UUID.randomUUID();
        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setSplitType(SplitType.PERCENTAGE);
        req.setPercentages(Map.of(u1, new BigDecimal("90.00")));

        assertThrows(IllegalArgumentException.class, () ->
            percentageStrategy.calculateSplits(new BigDecimal("100.00"), req)
        );
    }

    @Test
    @DisplayName("EXACT split: ₹60.00 and ₹40.00 on ₹100.00")
    void testExactSplit() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setSplitType(SplitType.EXACT);
        req.setExactAmounts(Map.of(
            u1, new BigDecimal("60.00"),
            u2, new BigDecimal("40.00")
        ));

        Map<UUID, BigDecimal> splits = exactStrategy.calculateSplits(new BigDecimal("100.00"), req);

        assertEquals(new BigDecimal("60.00"), splits.get(u1));
        assertEquals(new BigDecimal("40.00"), splits.get(u2));
    }

    @Test
    @DisplayName("EXACT split mismatch throws exception")
    void testExactSplitMismatch() {
        UUID u1 = UUID.randomUUID();
        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setSplitType(SplitType.EXACT);
        req.setExactAmounts(Map.of(u1, new BigDecimal("50.00")));

        assertThrows(IllegalArgumentException.class, () ->
            exactStrategy.calculateSplits(new BigDecimal("100.00"), req)
        );
    }

    @Test
    @DisplayName("SHARES split: 2 shares, 1 share on ₹90.00 (₹60.00 and ₹30.00)")
    void testSharesSplit() {
        UUID u1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID u2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setSplitType(SplitType.SHARES);
        req.setShares(Map.of(
            u1, 2,
            u2, 1
        ));

        Map<UUID, BigDecimal> splits = sharesStrategy.calculateSplits(new BigDecimal("90.00"), req);

        assertEquals(new BigDecimal("60.00"), splits.get(u1));
        assertEquals(new BigDecimal("30.00"), splits.get(u2));

        BigDecimal sum = splits.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("90.00"), sum);
    }

    @Test
    @DisplayName("ITEMIZED split: item breakdown + tax/tip proportional distribution")
    void testItemizedSplit() {
        UUID u1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID u2 = UUID.fromString("00000000-0000-0000-0000-000000000002");

        // Item 1: Burger (₹100) shared by u1 and u2 (₹50 each)
        // Item 2: Pizza (₹100) ordered by u1 only (₹100)
        // Total items subtotal: u1 = ₹150, u2 = ₹50 (Grand items total = ₹200)
        // Tax & Tip = ₹20 (u1 pays 75% = ₹15, u2 pays 25% = ₹5)
        // Final: u1 = ₹165.00, u2 = ₹55.00, Total = ₹220.00

        ItemRequest item1 = new ItemRequest("Burger", new BigDecimal("100.00"), Set.of(u1, u2));
        ItemRequest item2 = new ItemRequest("Pizza", new BigDecimal("100.00"), Set.of(u1));

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setSplitType(SplitType.ITEMIZED);
        req.setItems(List.of(item1, item2));
        req.setTaxAndTip(new BigDecimal("20.00"));

        Map<UUID, BigDecimal> splits = itemizedStrategy.calculateSplits(new BigDecimal("220.00"), req);

        assertEquals(new BigDecimal("165.00"), splits.get(u1));
        assertEquals(new BigDecimal("55.00"), splits.get(u2));

        BigDecimal sum = splits.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("220.00"), sum);
    }
}
