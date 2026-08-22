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
    private final MockPaymentGatewayClient mockPaymentGatewayClient;

    public SettlementService(SettlementRepository settlementRepository,
                             LedgerEntryRepository ledgerEntryRepository,
                             GroupSecurityGuard groupSecurityGuard,
                             MockPaymentGatewayClient mockPaymentGatewayClient) {
        this.settlementRepository = settlementRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.groupSecurityGuard = groupSecurityGuard;
        this.mockPaymentGatewayClient = mockPaymentGatewayClient;
    }

    /**
     * Records a debt settlement between two group members idempotently.
     * Integrates MockPaymentGatewayClient with retry and fallback handling.
     */
    @Transactional
    public SettlementResponse recordSettlement(UUID groupId,
                                               RecordSettlementRequest request,
                                               UUID requestingUserId) {
        // 1. Application-level idempotency check
        Optional<Settlement> existing = settlementRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return SettlementResponse.fromEntity(existing.get());
        }

        // 2. Verify group membership for requester, payer, and recipient
        groupSecurityGuard.checkMembership(groupId, requestingUserId);
        groupSecurityGuard.checkMembership(groupId, request.getFromUserId());
        groupSecurityGuard.checkMembership(groupId, request.getToUserId());

        if (request.getFromUserId().equals(request.getToUserId())) {
            throw new IllegalArgumentException("Cannot settle debt with oneself");
        }

        // 3. Process payment through mock gateway (handles @Retryable internally)
        PaymentResult paymentResult = mockPaymentGatewayClient.processPayment(
                request.getFromUserId(),
                request.getToUserId(),
                request.getAmount()
        );

        try {
            // 4. Save Settlement record with final status (COMPLETED or FAILED)
            Settlement settlement = new Settlement();
            settlement.setGroupId(groupId);
            settlement.setFromUserId(request.getFromUserId());
            settlement.setToUserId(request.getToUserId());
            settlement.setAmount(request.getAmount());
            settlement.setIdempotencyKey(request.getIdempotencyKey());
            settlement.setStatus(paymentResult.getStatus());

            Settlement savedSettlement = settlementRepository.saveAndFlush(settlement);

            // 5. Generate reversing LedgerEntry ONLY if payment succeeded
            if (paymentResult.isSuccessful()) {
                LedgerEntry ledgerEntry = new LedgerEntry();
                ledgerEntry.setGroupId(groupId);
                ledgerEntry.setFromUserId(request.getToUserId());
                ledgerEntry.setToUserId(request.getFromUserId());
                ledgerEntry.setAmount(request.getAmount());
                ledgerEntry.setCurrency("INR");
                ledgerEntry.setSourceType(SourceType.SETTLEMENT);
                ledgerEntry.setSourceId(savedSettlement.getId());

                ledgerEntryRepository.save(ledgerEntry);
            }

            return SettlementResponse.fromEntity(savedSettlement);
        } catch (DataIntegrityViolationException ex) {
            // Race condition caught by database unique constraint
            Settlement alreadySaved = settlementRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow(() -> ex);
            return SettlementResponse.fromEntity(alreadySaved);
        }
    }
}
