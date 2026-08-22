package com.settle.ledger;

import com.settle.group.GroupSecurityGuard;
import com.settle.ledger.dto.RecordSettlementRequest;
import com.settle.ledger.dto.SettlementResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final GroupSecurityGuard groupSecurityGuard;

    public SettlementService(SettlementRepository settlementRepository,
                             LedgerEntryRepository ledgerEntryRepository,
                             GroupSecurityGuard groupSecurityGuard) {
        this.settlementRepository = settlementRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.groupSecurityGuard = groupSecurityGuard;
    }

    /**
     * Records a debt settlement between two group members idempotently.
     *
     * Idempotency & Concurrency Strategy:
     * 1. Application Check: First check if a Settlement with this idempotencyKey already exists.
     *    If found, return it immediately.
     * 2. Database Constraint Defense: If two concurrent requests arrive simultaneously, both
     *    might pass the application check before either commits. The DB unique constraint on
     *    idempotency_key forces the second insert to fail with DataIntegrityViolationException.
     * 3. Graceful Recovery: We catch DataIntegrityViolationException, fetch the existing settlement
     *    inserted by the winning thread, and return it without surfacing an error.
     */
    @Transactional
    public SettlementResponse recordSettlement(UUID groupId,
                                               RecordSettlementRequest request,
                                               UUID requestingUserId) {
        // 1. Application-level check
        Optional<Settlement> existing = settlementRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return SettlementResponse.fromEntity(existing.get());
        }

        // 2. Verify membership for requester, payer, and recipient
        groupSecurityGuard.checkMembership(groupId, requestingUserId);
        groupSecurityGuard.checkMembership(groupId, request.getFromUserId());
        groupSecurityGuard.checkMembership(groupId, request.getToUserId());

        if (request.getFromUserId().equals(request.getToUserId())) {
            throw new IllegalArgumentException("Cannot settle debt with oneself");
        }

        try {
            // 3. Save Settlement record
            Settlement settlement = new Settlement();
            settlement.setGroupId(groupId);
            settlement.setFromUserId(request.getFromUserId());
            settlement.setToUserId(request.getToUserId());
            settlement.setAmount(request.getAmount());
            settlement.setIdempotencyKey(request.getIdempotencyKey());
            settlement.setStatus("COMPLETED");

            Settlement savedSettlement = settlementRepository.saveAndFlush(settlement);

            // 4. Generate reversing LedgerEntry in the same transaction
            // When fromUserId (debtor) pays toUserId (creditor):
            // We set LedgerEntry fromUserId = toUserId, toUserId = fromUserId.
            // In getNetBalances: toUserId gets -amount (reducing credit), fromUserId gets +amount (reducing debt).
            LedgerEntry ledgerEntry = new LedgerEntry();
            ledgerEntry.setGroupId(groupId);
            ledgerEntry.setFromUserId(request.getToUserId());
            ledgerEntry.setToUserId(request.getFromUserId());
            ledgerEntry.setAmount(request.getAmount());
            ledgerEntry.setCurrency("INR");
            ledgerEntry.setSourceType(SourceType.SETTLEMENT);
            ledgerEntry.setSourceId(savedSettlement.getId());

            ledgerEntryRepository.save(ledgerEntry);

            return SettlementResponse.fromEntity(savedSettlement);
        } catch (DataIntegrityViolationException ex) {
            // Race condition caught by database unique constraint
            Settlement alreadySaved = settlementRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow(() -> ex);
            return SettlementResponse.fromEntity(alreadySaved);
        }
    }
}
