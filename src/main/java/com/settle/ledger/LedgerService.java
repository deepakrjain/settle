package com.settle.ledger;

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
     *
     * For each LedgerEntry (fromUserId owes toUserId some amount):
     *   - toUserId's balance goes UP by that amount   (they are owed money)
     *   - fromUserId's balance goes DOWN by that amount (they owe money)
     *
     * A positive net balance means the user is a net creditor (others owe them).
     * A negative net balance means the user is a net debtor (they owe others).
     * The sum of all balances in a group is always zero — money is conserved.
     */
    @Transactional(readOnly = true)
    public List<UserBalance> getNetBalances(UUID groupId, UUID requestingUserId) {
        groupSecurityGuard.checkMembership(groupId, requestingUserId);

        List<LedgerEntry> entries = ledgerEntryRepository.findByGroupId(groupId);
        Map<UUID, BigDecimal> balanceMap = new HashMap<>();

        for (LedgerEntry entry : entries) {
            // toUserId is owed money → positive
            balanceMap.merge(entry.getToUserId(), entry.getAmount(), BigDecimal::add);
            // fromUserId owes money → negative
            balanceMap.merge(entry.getFromUserId(), entry.getAmount().negate(), BigDecimal::add);
        }

        return balanceMap.entrySet().stream()
                .map(e -> new UserBalance(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(UserBalance::getUserId))
                .collect(Collectors.toList());
    }
}
