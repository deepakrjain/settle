package com.settle.expense;

import com.settle.expense.dto.CreateExpenseRequest;
import com.settle.expense.dto.ExpenseResponse;
import com.settle.expense.strategy.SplitStrategy;
import com.settle.expense.strategy.SplitStrategyFactory;
import com.settle.group.GroupSecurityGuard;
import com.settle.ledger.GroupBalanceUpdatedEvent;
import com.settle.ledger.LedgerEntry;
import com.settle.ledger.LedgerEntryRepository;
import com.settle.ledger.SourceType;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public ExpenseService(ExpenseRepository expenseRepository,
                          GroupSecurityGuard groupSecurityGuard,
                          SplitStrategyFactory splitStrategyFactory,
                          LedgerEntryRepository ledgerEntryRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.expenseRepository = expenseRepository;
        this.groupSecurityGuard = groupSecurityGuard;
        this.splitStrategyFactory = splitStrategyFactory;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.eventPublisher = eventPublisher;
    }

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

        // 4. Calculate total amount
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

        // 6. Generate ledger entries
        UUID payerId = request.getPaidByUserId();
        List<LedgerEntry> ledgerEntries = new ArrayList<>();

        for (Map.Entry<UUID, BigDecimal> entry : splitMap.entrySet()) {
            UUID participantId = entry.getKey();
            BigDecimal shareAmount = entry.getValue();

            if (participantId.equals(payerId)) {
                continue;
            }

            LedgerEntry ledgerEntry = new LedgerEntry();
            ledgerEntry.setGroupId(groupId);
            ledgerEntry.setFromUserId(participantId);
            ledgerEntry.setToUserId(payerId);
            ledgerEntry.setAmount(shareAmount);
            ledgerEntry.setCurrency(request.getCurrency());
            ledgerEntry.setSourceType(SourceType.EXPENSE);
            ledgerEntry.setSourceId(savedExpense.getId());

            ledgerEntries.add(ledgerEntry);
        }

        if (!ledgerEntries.isEmpty()) {
            ledgerEntryRepository.saveAll(ledgerEntries);
            // Publish event to trigger WebSocket broadcast AFTER transaction commits
            eventPublisher.publishEvent(new GroupBalanceUpdatedEvent(groupId));
        }

        return ExpenseResponse.fromEntity(savedExpense);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getGroupExpenses(UUID groupId, Pageable pageable, UUID requestingUserId) {
        groupSecurityGuard.checkMembership(groupId, requestingUserId);
        Page<Expense> expensesPage = expenseRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable);
        return expensesPage.map(ExpenseResponse::fromEntity);
    }
}
