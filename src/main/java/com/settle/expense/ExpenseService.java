package com.settle.expense;

import com.settle.expense.dto.CreateExpenseRequest;
import com.settle.expense.dto.ExpenseResponse;
import com.settle.expense.strategy.SplitStrategy;
import com.settle.expense.strategy.SplitStrategyFactory;
import com.settle.group.GroupSecurityGuard;
import com.settle.ledger.LedgerEntry;
import com.settle.ledger.LedgerEntryRepository;
import com.settle.ledger.SourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupSecurityGuard groupSecurityGuard;
    private final SplitStrategyFactory splitStrategyFactory;
    private final LedgerEntryRepository ledgerEntryRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          GroupSecurityGuard groupSecurityGuard,
                          SplitStrategyFactory splitStrategyFactory,
                          LedgerEntryRepository ledgerEntryRepository) {
        this.expenseRepository = expenseRepository;
        this.groupSecurityGuard = groupSecurityGuard;
        this.splitStrategyFactory = splitStrategyFactory;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Creates an expense AND its corresponding ledger entries in a SINGLE transaction.
     *
     * Why one transaction? If these were two separate transactions and the process crashed
     * after saving the expense but before writing ledger entries, the system would show
     * the expense in the group's history but the balances would be wrong — participants
     * would see an expense they supposedly split but their debts would never have been
     * recorded. Conversely, if ledger entries were written but the expense save failed,
     * users would owe money for a phantom expense that doesn't appear anywhere. Either
     * scenario leaves the system in an inconsistent state that is very hard to detect
     * and repair. A single @Transactional boundary guarantees all-or-nothing.
     */
    @Transactional
    public ExpenseResponse createExpense(UUID groupId,
                                         CreateExpenseRequest request,
                                         UUID requestingUserId) {
        // 1. Verify requesting user and payer are group members
        groupSecurityGuard.checkMembership(groupId, requestingUserId);
        groupSecurityGuard.checkMembership(groupId, request.getPaidByUserId());

        // 2. Select strategy and calculate splits
        SplitStrategy strategy = splitStrategyFactory.getStrategy(request.getSplitType());
        Map<UUID, BigDecimal> splitMap = strategy.calculateSplits(request.getAmount(), request);

        // 3. Verify all split participant user IDs are group members
        for (UUID participantId : splitMap.keySet()) {
            groupSecurityGuard.checkMembership(groupId, participantId);
        }

        // 4. Calculate total amount (if not explicitly provided, derived from split map)
        BigDecimal totalAmount = request.getAmount();
        if (totalAmount == null) {
            totalAmount = splitMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // 5. Construct and populate Expense + ExpenseSplit entities
        Expense expense = new Expense();
        expense.setGroupId(groupId);
        expense.setPaidByUserId(request.getPaidByUserId());
        expense.setAmount(totalAmount);
        expense.setCurrency(request.getCurrency());
        expense.setDescription(request.getDescription());
        expense.setCategory(request.getCategory());
        expense.setSplitType(request.getSplitType());

        for (Map.Entry<UUID, BigDecimal> entry : splitMap.entrySet()) {
            ExpenseSplit split = new ExpenseSplit();
            split.setUserId(entry.getKey());
            split.setShareAmount(entry.getValue());
            expense.addSplit(split);
        }

        Expense savedExpense = expenseRepository.save(expense);

        // 6. Generate ledger entries: each participant who isn't the payer owes the payer their share
        UUID payerId = request.getPaidByUserId();
        List<LedgerEntry> ledgerEntries = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : splitMap.entrySet()) {
            UUID participantId = entry.getKey();
            BigDecimal shareAmount = entry.getValue();

            // Skip the payer — they don't owe themselves
            if (participantId.equals(payerId)) {
                continue;
            }

            LedgerEntry ledgerEntry = new LedgerEntry();
            ledgerEntry.setGroupId(groupId);
            ledgerEntry.setFromUserId(participantId);   // participant owes...
            ledgerEntry.setToUserId(payerId);            // ...the payer
            ledgerEntry.setAmount(shareAmount);
            ledgerEntry.setCurrency(request.getCurrency());
            ledgerEntry.setSourceType(SourceType.EXPENSE);
            ledgerEntry.setSourceId(savedExpense.getId());

            ledgerEntries.add(ledgerEntry);
        }

        if (!ledgerEntries.isEmpty()) {
            ledgerEntryRepository.saveAll(ledgerEntries);
        }

        return ExpenseResponse.fromEntity(savedExpense);
    }

    /**
     * Retrieves paginated list of expenses for a group.
     * Verifies requesting user is a group member.
     */
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getGroupExpenses(UUID groupId, Pageable pageable, UUID requestingUserId) {
        groupSecurityGuard.checkMembership(groupId, requestingUserId);
        Page<Expense> expensesPage = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
        return expensesPage.map(ExpenseResponse::fromEntity);
    }
}
