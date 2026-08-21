package com.settle.expense;

import com.settle.expense.dto.CreateExpenseRequest;
import com.settle.expense.dto.ExpenseResponse;
import com.settle.expense.strategy.SplitStrategy;
import com.settle.expense.strategy.SplitStrategyFactory;
import com.settle.group.GroupSecurityGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupSecurityGuard groupSecurityGuard;
    private final SplitStrategyFactory splitStrategyFactory;

    public ExpenseService(ExpenseRepository expenseRepository,
                          GroupSecurityGuard groupSecurityGuard,
                          SplitStrategyFactory splitStrategyFactory) {
        this.expenseRepository = expenseRepository;
        this.groupSecurityGuard = groupSecurityGuard;
        this.splitStrategyFactory = splitStrategyFactory;
    }

    /**
     * Creates an expense using the specified SplitStrategy (EQUAL, PERCENTAGE, EXACT, SHARES, ITEMIZED).
     * Verifies that the requesting user, payer, and all split participants belong to the group.
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
