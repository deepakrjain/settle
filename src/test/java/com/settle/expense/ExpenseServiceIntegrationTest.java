package com.settle.expense;

import com.settle.AbstractIntegrationTest;
import com.settle.expense.dto.CreateExpenseRequest;
import com.settle.expense.dto.ExpenseResponse;
import com.settle.group.Group;
import com.settle.group.GroupMember;
import com.settle.group.GroupMemberRepository;
import com.settle.group.GroupRepository;
import com.settle.ledger.LedgerEntry;
import com.settle.ledger.LedgerEntryRepository;
import com.settle.user.User;
import com.settle.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    private User user1;
    private User user2;
    private Group group;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();

        user1 = new User();
        user1.setEmail("integration1_" + System.currentTimeMillis() + "@test.com");
        user1.setPasswordHash("hash");
        user1.setDisplayName("Integration User 1");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setEmail("integration2_" + System.currentTimeMillis() + "@test.com");
        user2.setPasswordHash("hash");
        user2.setDisplayName("Integration User 2");
        user2 = userRepository.save(user2);

        group = new Group();
        group.setName("Integration Trip Group");
        group.setCreatedBy(user1.getId());
        group = groupRepository.save(group);

        GroupMember m1 = new GroupMember();
        m1.setGroupId(group.getId());
        m1.setUserId(user1.getId());
        groupMemberRepository.save(m1);

        GroupMember m2 = new GroupMember();
        m2.setGroupId(group.getId());
        m2.setUserId(user2.getId());
        groupMemberRepository.save(m2);
    }

    @Test
    @Transactional
    @DisplayName("End-to-End Expense Creation: Expense, ExpenseSplit, and LedgerEntry rows are saved atomically in Postgres")
    void testEndToEndExpenseCreation() {
        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("INR");
        request.setDescription("Dinner");
        request.setPaidByUserId(user1.getId());
        request.setSplitType(SplitType.EQUAL);
        request.setParticipantUserIds(Set.of(user1.getId(), user2.getId()));

        ExpenseResponse response = expenseService.createExpense(group.getId(), request, user1.getId());

        assertNotNull(response.getId());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals(2, response.getSplits().size());

        // Verify real PostgreSQL database rows
        Expense savedExpense = expenseRepository.findById(response.getId()).orElse(null);
        assertNotNull(savedExpense);
        assertEquals("Dinner", savedExpense.getDescription());
        assertEquals(2, savedExpense.getSplits().size());

        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findByGroupId(group.getId());
        assertEquals(1, ledgerEntries.size()); // user2 owes user1 ₹50.00
        LedgerEntry entry = ledgerEntries.get(0);
        assertEquals(user2.getId(), entry.getFromUserId());
        assertEquals(user1.getId(), entry.getToUserId());
        assertEquals(new BigDecimal("50.00"), entry.getAmount());
    }

    @Test
    @DisplayName("@Transactional Rollback Proof: failure partway through expense creation rolls back ALL database writes")
    void testTransactionalRollbackProof() {
        long initialExpenseCount = expenseRepository.count();
        long initialLedgerCount = ledgerEntryRepository.findByGroupId(group.getId()).size();

        User nonMemberUser = new User();
        nonMemberUser.setEmail("outsider_" + System.currentTimeMillis() + "@test.com");
        nonMemberUser.setPasswordHash("hash");
        nonMemberUser.setDisplayName("Outsider");
        nonMemberUser = userRepository.save(nonMemberUser);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setAmount(new BigDecimal("200.00"));
        request.setCurrency("INR");
        request.setDescription("Faulty Expense");
        request.setPaidByUserId(user1.getId());
        request.setSplitType(SplitType.EQUAL);
        // Include nonMemberUser who triggers AccessDeniedException during participant security verification
        request.setParticipantUserIds(Set.of(user1.getId(), nonMemberUser.getId()));

        assertThrows(AccessDeniedException.class, () ->
            expenseService.createExpense(group.getId(), request, user1.getId())
        );

        // Verify ATOMICITY: ZERO rows written to expenses, expense_splits, or ledger_entries!
        assertEquals(initialExpenseCount, expenseRepository.count());
        assertEquals(initialLedgerCount, ledgerEntryRepository.findByGroupId(group.getId()).size());
    }
}
