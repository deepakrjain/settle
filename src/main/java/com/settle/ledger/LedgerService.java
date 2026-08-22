package com.settle.ledger;

import com.settle.ledger.dto.SettlementPlanResponse;
import com.settle.ledger.dto.SettlementTransaction;
import com.settle.ledger.dto.UserBalance;
import com.settle.group.GroupSecurityGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final GroupSecurityGuard groupSecurityGuard;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository,
                         GroupSecurityGuard groupSecurityGuard) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.groupSecurityGuard = groupSecurityGuard;
    }

    /**
     * Computes the net balance for every user who appears in the group's ledger.
     */
    @Transactional(readOnly = true)
    public List<UserBalance> getNetBalances(UUID groupId, UUID requestingUserId) {
        groupSecurityGuard.checkMembership(groupId, requestingUserId);

        List<LedgerEntry> entries = ledgerEntryRepository.findByGroupId(groupId);
        Map<UUID, BigDecimal> balanceMap = new HashMap<>();

        for (LedgerEntry entry : entries) {
            balanceMap.merge(entry.getToUserId(), entry.getAmount(), BigDecimal::add);
            balanceMap.merge(entry.getFromUserId(), entry.getAmount().negate(), BigDecimal::add);
        }

        return balanceMap.entrySet().stream()
                .map(e -> new UserBalance(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(UserBalance::getUserId))
                .collect(Collectors.toList());
    }

    /**
     * Calculates the settlement plan for a group.
     * Always computes the Greedy settlement plan.
     * Computes the Optimal plan if non-zero member count is under 10.
     */
    @Transactional(readOnly = true)
    public SettlementPlanResponse getSettlementPlan(UUID groupId, UUID requestingUserId) {
        groupSecurityGuard.checkMembership(groupId, requestingUserId);

        List<UserBalance> userBalances = getNetBalances(groupId, requestingUserId);
        Map<UUID, BigDecimal> balanceMap = userBalances.stream()
                .collect(Collectors.toMap(UserBalance::getUserId, UserBalance::getNetBalance));

        List<SettlementTransaction> greedyPlan = SettlementCalculator.calculateGreedy(balanceMap);

        long nonZeroCount = balanceMap.values().stream()
                .filter(val -> val != null && val.compareTo(BigDecimal.ZERO) != 0)
                .count();

        List<SettlementTransaction> optimalPlan = null;
        Integer optimalCount = null;
        boolean optimalCalculated = false;

        if (nonZeroCount > 0 && nonZeroCount < 10) {
            optimalPlan = SettlementCalculator.calculateOptimal(balanceMap);
            optimalCount = optimalPlan.size();
            optimalCalculated = true;
        }

        return new SettlementPlanResponse(
                greedyPlan,
                greedyPlan.size(),
                optimalPlan,
                optimalCount,
                optimalCalculated
        );
    }
}
