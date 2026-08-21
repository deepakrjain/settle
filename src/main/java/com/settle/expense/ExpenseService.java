package com.settle.expense;

import com.settle.expense.dto.CreateExpenseRequest;
import com.settle.expense.dto.ExpenseResponse;
import com.settle.group.GroupSecurityGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupSecurityGuard groupSecurityGuard;

    public ExpenseService(ExpenseRepository expenseRepository,
                          GroupSecurityGuard groupSecurityGuard) {
        this.expenseRepository = expenseRepository;
        this.groupSecurityGuard = groupSecurityGuard;
    }

    /**
     * Creates an expense with equal splits among participant user IDs.
     * Verifies that the requesting user, payer, and all participants belong to the group.
     */
    @Transactional
    public ExpenseResponse createExpense(UUID groupId,
                                         CreateExpenseRequest request,
                                         UUID requestingUserId) {
        // 1. Verify requesting user is a member of the group
        groupSecurityGuard.checkMembership(groupId, requestingUserId);

        // 2. Verify paidByUserId is a member of the group
        groupSecurityGuard.checkMembership(groupId, request.getPaidByUserId());

        // 3. Verify all participants are members of the group
        Set<UUID> participants = request.getParticipantUserIds();
        for (UUID participantId : participants) {
            groupSecurityGuard.checkMembership(groupId, participantId);
        }

        // 4. Calculate equal splits with remainder allocation
        Map<UUID, BigDecimal> splitMap = EqualSplitCalculator.calculateSplits(request.getAmount(), participants);

        // 5. Construct and populate Expense + ExpenseSplit entities
        Expense expense = new Expense();
        expense.setGroupId(groupId);
        expense.setPaidByUserId(request.getPaidByUserId());
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency());
        expense.setDescription(request.getDescription());
        expense.setCategory(request.getCategory());

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
