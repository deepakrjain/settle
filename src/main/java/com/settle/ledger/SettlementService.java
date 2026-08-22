package com.settle.ledger;

import com.settle.group.GroupSecurityGuard;
import com.settle.ledger.dto.RecordSettlementRequest;
import com.settle.ledger.dto.SettlementResponse;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public SettlementService(SettlementRepository settlementRepository,
                             LedgerEntryRepository ledgerEntryRepository,
                             GroupSecurityGuard groupSecurityGuard,
                             MockPaymentGatewayClient mockPaymentGatewayClient,
                             ApplicationEventPublisher eventPublisher) {
        this.settlementRepository = settlementRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.groupSecurityGuard = groupSecurityGuard;
        this.mockPaymentGatewayClient = mockPaymentGatewayClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public SettlementResponse recordSettlement(UUID groupId,
                                               RecordSettlementRequest request,
                                               UUID requestingUserId) {
        Optional<Settlement> existing = settlementRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return SettlementResponse.fromEntity(existing.get());
        }

        groupSecurityGuard.checkMembership(groupId, requestingUserId);
        groupSecurityGuard.checkMembership(groupId, request.getFromUserId());
        groupSecurityGuard.checkMembership(groupId, request.getToUserId());

        if (request.getFromUserId().equals(request.getToUserId())) {
            throw new IllegalArgumentException("Cannot settle debt with oneself");
        }

        PaymentResult paymentResult = mockPaymentGatewayClient.processPayment(
                request.getFromUserId(),
                request.getToUserId(),
                request.getAmount()
        );

        try {
            Settlement settlement = new Settlement();
            settlement.setGroupId(groupId);
            settlement.setFromUserId(request.getFromUserId());
            settlement.setToUserId(request.getToUserId());
            settlement.setAmount(request.getAmount());
            settlement.setIdempotencyKey(request.getIdempotencyKey());
            settlement.setStatus(paymentResult.getStatus());

            Settlement savedSettlement = settlementRepository.saveAndFlush(settlement);

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
                // Publish event to trigger WebSocket broadcast AFTER transaction commits
                eventPublisher.publishEvent(new GroupBalanceUpdatedEvent(groupId));
            }

            return SettlementResponse.fromEntity(savedSettlement);
        } catch (DataIntegrityViolationException ex) {
            Settlement alreadySaved = settlementRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .orElseThrow(() -> ex);
            return SettlementResponse.fromEntity(alreadySaved);
        }
    }
}
