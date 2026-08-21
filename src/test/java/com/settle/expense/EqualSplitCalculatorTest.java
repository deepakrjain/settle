package com.settle.expense;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EqualSplitCalculatorTest {

    @Test
    @DisplayName("Exact division without remainder (e.g. ₹100 among 4 users)")
    void testExactDivision() {
        BigDecimal total = new BigDecimal("100.00");
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID user3 = UUID.randomUUID();
        UUID user4 = UUID.randomUUID();

        Set<UUID> participants = Set.of(user1, user2, user3, user4);
        Map<UUID, BigDecimal> splits = EqualSplitCalculator.calculateSplits(total, participants);

        assertEquals(4, splits.size());
        assertEquals(new BigDecimal("25.00"), splits.get(user1));
        assertEquals(new BigDecimal("25.00"), splits.get(user2));
        assertEquals(new BigDecimal("25.00"), splits.get(user3));
        assertEquals(new BigDecimal("25.00"), splits.get(user4));

        BigDecimal sum = splits.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(total, sum);
    }

    @Test
    @DisplayName("Division with remainder (e.g. ₹100 among 3 users: 33.34, 33.33, 33.33)")
    void testRemainderDivisionThreeWays() {
        BigDecimal total = new BigDecimal("100.00");
        UUID u1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID u2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID u3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

        Set<UUID> participants = Set.of(u3, u1, u2); // unordered input
        Map<UUID, BigDecimal> splits = EqualSplitCalculator.calculateSplits(total, participants);

        assertEquals(3, splits.size());

        // u1 comes first in natural sort order -> gets ₹33.34
        assertEquals(new BigDecimal("33.34"), splits.get(u1));
        assertEquals(new BigDecimal("33.33"), splits.get(u2));
        assertEquals(new BigDecimal("33.33"), splits.get(u3));

        BigDecimal sum = splits.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(total, sum);
    }

    @Test
    @DisplayName("Division with 4-paisa remainder (e.g. ₹100 among 6 users: 16.67 x 4, 16.66 x 2)")
    void testRemainderDivisionSixWays() {
        BigDecimal total = new BigDecimal("100.00");
        UUID u1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID u2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID u3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID u4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
        UUID u5 = UUID.fromString("00000000-0000-0000-0000-000000000005");
        UUID u6 = UUID.fromString("00000000-0000-0000-0000-000000000006");

        Set<UUID> participants = Set.of(u1, u2, u3, u4, u5, u6);
        Map<UUID, BigDecimal> splits = EqualSplitCalculator.calculateSplits(total, participants);

        // 100 / 6 = 16.66 base share. Total base = 99.96. Remainder = 0.04 (4 paisa).
        // First 4 users get 16.67, last 2 get 16.66.
        assertEquals(new BigDecimal("16.67"), splits.get(u1));
        assertEquals(new BigDecimal("16.67"), splits.get(u2));
        assertEquals(new BigDecimal("16.67"), splits.get(u3));
        assertEquals(new BigDecimal("16.67"), splits.get(u4));
        assertEquals(new BigDecimal("16.66"), splits.get(u5));
        assertEquals(new BigDecimal("16.66"), splits.get(u6));

        BigDecimal sum = splits.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(total, sum);
    }
}
