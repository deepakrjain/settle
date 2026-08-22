package com.settle.ledger;

import com.settle.ledger.dto.SettlementTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SettlementCalculatorTest {

    @Test
    @DisplayName("Simple 3-way balance: Greedy and Optimal produce 2 transactions")
    void testSimpleThreeWaySettlement() {
        UUID u1 = UUID.randomUUID(); // owes 50
        UUID u2 = UUID.randomUUID(); // owes 50
        UUID u3 = UUID.randomUUID(); // is owed 100

        Map<UUID, BigDecimal> balances = Map.of(
            u1, new BigDecimal("-50.00"),
            u2, new BigDecimal("-50.00"),
            u3, new BigDecimal("100.00")
        );

        List<SettlementTransaction> greedy = SettlementCalculator.calculateGreedy(balances);
        List<SettlementTransaction> optimal = SettlementCalculator.calculateOptimal(balances);

        assertEquals(2, greedy.size());
        assertEquals(2, optimal.size());

        verifyZeroSum(balances, greedy);
        verifyZeroSum(balances, optimal);
    }

    @Test
    @DisplayName("Zero-sum subset settlement: 4 users where subsets {U1, U3} and {U2, U4} settle independently")
    void testZeroSumSubsetSettlement() {
        UUID u1 = UUID.fromString("00000000-0000-0000-0000-000000000001"); // +10
        UUID u2 = UUID.fromString("00000000-0000-0000-0000-000000000002"); // +20
        UUID u3 = UUID.fromString("00000000-0000-0000-0000-000000000003"); // -10
        UUID u4 = UUID.fromString("00000000-0000-0000-0000-000000000004"); // -20

        Map<UUID, BigDecimal> balances = Map.of(
            u1, new BigDecimal("10.00"),
            u2, new BigDecimal("20.00"),
            u3, new BigDecimal("-10.00"),
            u4, new BigDecimal("-20.00")
        );

        List<SettlementTransaction> greedy = SettlementCalculator.calculateGreedy(balances);
        List<SettlementTransaction> optimal = SettlementCalculator.calculateOptimal(balances);

        // Optimal should find the 2 zero-sum partitions and produce exactly 2 transactions
        assertEquals(2, optimal.size());
        assertTrue(greedy.size() <= 3);

        verifyZeroSum(balances, greedy);
        verifyZeroSum(balances, optimal);
    }

    @Test
    @DisplayName("Complex 5-way balance verification")
    void testComplexFiveWaySettlement() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        UUID u3 = UUID.randomUUID();
        UUID u4 = UUID.randomUUID();
        UUID u5 = UUID.randomUUID();

        Map<UUID, BigDecimal> balances = Map.of(
            u1, new BigDecimal("30.00"),
            u2, new BigDecimal("40.00"),
            u3, new BigDecimal("-20.00"),
            u4, new BigDecimal("-30.00"),
            u5, new BigDecimal("-20.00")
        );

        List<SettlementTransaction> greedy = SettlementCalculator.calculateGreedy(balances);
        List<SettlementTransaction> optimal = SettlementCalculator.calculateOptimal(balances);

        assertFalse(greedy.isEmpty());
        assertFalse(optimal.isEmpty());

        verifyZeroSum(balances, greedy);
        verifyZeroSum(balances, optimal);
    }

    private void verifyZeroSum(Map<UUID, BigDecimal> initialBalances, List<SettlementTransaction> transactions) {
        Map<UUID, BigDecimal> workingBalances = new HashMap<>(initialBalances);

        for (SettlementTransaction tx : transactions) {
            // fromUser pays toUser amount
            workingBalances.merge(tx.getFromUserId(), tx.getAmount(), BigDecimal::add);
            workingBalances.merge(tx.getToUserId(), tx.getAmount().negate(), BigDecimal::add);
        }

        for (Map.Entry<UUID, BigDecimal> entry : workingBalances.entrySet()) {
            assertEquals(0, entry.getValue().compareTo(BigDecimal.ZERO),
                "User " + entry.getKey() + " balance should be 0.00 after settlement");
        }
    }
}
