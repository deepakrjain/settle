package com.settle.ledger;

import com.settle.AbstractIntegrationTest;
import com.settle.group.Group;
import com.settle.group.GroupMember;
import com.settle.group.GroupMemberRepository;
import com.settle.group.GroupRepository;
import com.settle.ledger.dto.RecordSettlementRequest;
import com.settle.ledger.dto.SettlementResponse;
import com.settle.user.User;
import com.settle.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SettlementServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SettlementService settlementService;

    @Autowired
    private SettlementRepository settlementRepository;

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
        settlementRepository.deleteAll();

        user1 = new User();
        user1.setEmail("settle_int1_" + System.currentTimeMillis() + "@test.com");
        user1.setPasswordHash("hash");
        user1.setDisplayName("Settlement User 1");
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setEmail("settle_int2_" + System.currentTimeMillis() + "@test.com");
        user2.setPasswordHash("hash");
        user2.setDisplayName("Settlement User 2");
        user2 = userRepository.save(user2);

        group = new Group();
        group.setName("Settlement Group");
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
    @DisplayName("Idempotent Settlement in Real Postgres: Duplicate idempotencyKey requests produce exactly 1 Settlement and 1 LedgerEntry row")
    void testRealDatabaseIdempotencyAndUniqueConstraint() {
        String idempotencyKey = "IDEM-REAL-" + System.currentTimeMillis();

        RecordSettlementRequest req = new RecordSettlementRequest();
        req.setFromUserId(user1.getId());
        req.setToUserId(user2.getId());
        req.setAmount(new BigDecimal("75.00"));
        req.setIdempotencyKey(idempotencyKey);

        // First call
        SettlementResponse resp1 = settlementService.recordSettlement(group.getId(), req, user1.getId());

        // Duplicate call with exact same idempotencyKey
        SettlementResponse resp2 = settlementService.recordSettlement(group.getId(), req, user1.getId());

        // Assert both responses refer to the exact same Settlement entity ID
        assertEquals(resp1.getId(), resp2.getId());

        // Verify REAL PostgreSQL database state: exactly 1 Settlement and 1 LedgerEntry row exist!
        List<Settlement> settlements = settlementRepository.findAll();
        assertEquals(1, settlements.size());
        assertEquals(idempotencyKey, settlements.get(0).getIdempotencyKey());

        List<LedgerEntry> ledgerEntries = ledgerEntryRepository.findByGroupId(group.getId());
        assertEquals(1, ledgerEntries.size());
    }
}
