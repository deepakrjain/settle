package com.settle.ledger;

import com.settle.ledger.dto.SettlementTransaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class SettlementCalculator {

    /**
     * Greedy settlement algorithm.
     * Repeatedly settles the maximum debtor with the maximum creditor.
     * Time Complexity: O(N log N) with PriorityQueue.
     */
    public static List<SettlementTransaction> calculateGreedy(Map<UUID, BigDecimal> netBalances) {
        List<SettlementTransaction> transactions = new ArrayList<>();

        Map<UUID, BigDecimal> balances = new HashMap<>();
        for (Map.Entry<UUID, BigDecimal> entry : netBalances.entrySet()) {
            if (entry.getValue() != null && entry.getValue().compareTo(BigDecimal.ZERO) != 0) {
                balances.put(entry.getKey(), entry.getValue());
            }
        }

        PriorityQueue<Map.Entry<UUID, BigDecimal>> debtors = new PriorityQueue<>(
                Comparator.comparing(Map.Entry::getValue)
        );
        PriorityQueue<Map.Entry<UUID, BigDecimal>> creditors = new PriorityQueue<>(
                (e1, e2) -> e2.getValue().compareTo(e1.getValue())
        );

        for (Map.Entry<UUID, BigDecimal> entry : balances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()));
            }
        }

        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Map.Entry<UUID, BigDecimal> debtor = debtors.poll();
            Map.Entry<UUID, BigDecimal> creditor = creditors.poll();

            BigDecimal debt = debtor.getValue().abs();
            BigDecimal credit = creditor.getValue();

            BigDecimal settleAmount = debt.min(credit).setScale(2, RoundingMode.HALF_UP);

            transactions.add(new SettlementTransaction(debtor.getKey(), creditor.getKey(), settleAmount));

            BigDecimal remainingDebt = debt.subtract(settleAmount);
            BigDecimal remainingCredit = credit.subtract(settleAmount);

            if (remainingDebt.compareTo(BigDecimal.ZERO) > 0) {
                debtors.add(new AbstractMap.SimpleEntry<>(debtor.getKey(), remainingDebt.negate()));
            }
            if (remainingCredit.compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new AbstractMap.SimpleEntry<>(creditor.getKey(), remainingCredit));
            }
        }

        return transactions;
    }

    /**
     * Exact optimal settlement algorithm using subset-sum / recursive backtracking.
     * Finds the absolute minimum transaction count by discovering zero-sum subsets.
     * Time Complexity: Exponential O(2^N). Recommended only when non-zero balances count < 10.
     */
    public static List<SettlementTransaction> calculateOptimal(Map<UUID, BigDecimal> netBalances) {
        Map<UUID, BigDecimal> nonZero = new HashMap<>();
        for (Map.Entry<UUID, BigDecimal> entry : netBalances.entrySet()) {
            if (entry.getValue() != null && entry.getValue().compareTo(BigDecimal.ZERO) != 0) {
                nonZero.put(entry.getKey(), entry.getValue());
            }
        }

        if (nonZero.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> users = new ArrayList<>(nonZero.keySet());
        List<BigDecimal> amounts = new ArrayList<>();
        for (UUID u : users) {
            amounts.add(nonZero.get(u));
        }

        List<SettlementTransaction> result = new ArrayList<>();
        solveOptimalBacktrack(users, amounts, result);
        return result;
    }

    private static void solveOptimalBacktrack(List<UUID> users, List<BigDecimal> amounts, List<SettlementTransaction> result) {
        int firstNonZero = -1;
        for (int i = 0; i < amounts.size(); i++) {
            if (amounts.get(i).compareTo(BigDecimal.ZERO) != 0) {
                firstNonZero = i;
                break;
            }
        }

        if (firstNonZero == -1) {
            return;
        }

        int n = amounts.size();
        List<SettlementTransaction> bestPath = null;

        for (int j = firstNonZero + 1; j < n; j++) {
            if (amounts.get(firstNonZero).signum() != amounts.get(j).signum() && amounts.get(j).compareTo(BigDecimal.ZERO) != 0) {
                List<BigDecimal> nextAmounts = new ArrayList<>(amounts);

                BigDecimal valI = nextAmounts.get(firstNonZero);
                BigDecimal valJ = nextAmounts.get(j);

                UUID from, to;
                BigDecimal settleAmt;

                if (valI.compareTo(BigDecimal.ZERO) < 0) {
                    from = users.get(firstNonZero);
                    to = users.get(j);
                    settleAmt = valI.abs().min(valJ);
                } else {
                    from = users.get(j);
                    to = users.get(firstNonZero);
                    settleAmt = valJ.abs().min(valI);
                }

                nextAmounts.set(firstNonZero, valI.add(valI.compareTo(BigDecimal.ZERO) < 0 ? settleAmt : settleAmt.negate()));
                nextAmounts.set(j, valJ.add(valJ.compareTo(BigDecimal.ZERO) < 0 ? settleAmt : settleAmt.negate()));

                List<SettlementTransaction> currentPath = new ArrayList<>();
                currentPath.add(new SettlementTransaction(from, to, settleAmt));
                solveOptimalBacktrack(users, nextAmounts, currentPath);

                if (bestPath == null || currentPath.size() < bestPath.size()) {
                    bestPath = currentPath;
                }

                if (valI.add(valJ).compareTo(BigDecimal.ZERO) == 0) {
                    break;
                }
            }
        }

        if (bestPath != null) {
            result.addAll(bestPath);
        } else {
            result.addAll(calculateGreedy(createBalanceMap(users, amounts)));
        }
    }

    private static Map<UUID, BigDecimal> createBalanceMap(List<UUID> users, List<BigDecimal> amounts) {
        Map<UUID, BigDecimal> map = new HashMap<>();
        for (int i = 0; i < users.size(); i++) {
            map.put(users.get(i), amounts.get(i));
        }
        return map;
    }
}
