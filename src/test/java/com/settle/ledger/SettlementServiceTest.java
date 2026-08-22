package com.settle.ledger;

import com.settle.group.GroupSecurityGuard;
import com.settle.ledger.dto.RecordSettlementRequest;
import com.settle.ledger.dto.SettlementResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private GroupSecurityGuard groupSecurityGuard;

    @Mock
    private MockPaymentGatewayClient mockPaymentGatewayClient;

    @InjectMocks
    private SettlementService settlementService;

    private UUID groupId;
    private UUID fromUserId;
    private UUID toUserId;
    private RecordSettlementRequest request;

    @BeforeEach
    void setUp() {
        groupId = UUID.randomUUID();
        fromUserId = UUID.randomUUID();
        toUserId = UUID.randomUUID();

        request = new RecordSettlementRequest();
        request.setFromUserId(fromUserId);
        request.setToUserId(toUserId);
        request.setAmount(new BigDecimal("50.00"));
        request.setIdempotencyKey("KEY-999");
    }

    @Test
    @DisplayName("Idempotent check: returning existing settlement when key already exists")
    void testExistingIdempotencyKeyReturnsExisting() {
        Settlement existing = new Settlement();
        existing.setId(UUID.randomUUID());
        existing.setGroupId(groupId);
        existing.setFromUserId(fromUserId);
        existing.setToUserId(toUserId);
        existing.setAmount(new BigDecimal("50.00"));
        existing.setIdempotencyKey("KEY-999");

        when(settlementRepository.findByIdempotencyKey("KEY-999")).thenReturn(Optional.of(existing));

        SettlementResponse response = settlementService.recordSettlement(groupId, request, fromUserId);

        assertNotNull(response);
        assertEquals(existing.getId(), response.getId());
        verify(mockPaymentGatewayClient, never()).processPayment(any(), any(), any());
        verify(settlementRepository, never()).saveAndFlush(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Successful payment records COMPLETED settlement and reversing ledger entry")
    void testSuccessfulPaymentRecordsLedgerEntry() {
        when(settlementRepository.findByIdempotencyKey("KEY-999")).thenReturn(Optional.empty());
        when(mockPaymentGatewayClient.processPayment(any(), any(), any()))
                .thenReturn(new PaymentResult(true, "TXN-123", "COMPLETED", null));
        when(settlementRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        SettlementResponse response = settlementService.recordSettlement(groupId, request, fromUserId);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        verify(ledgerEntryRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Failed payment records FAILED settlement and DOES NOT create ledger entry")
    void testFailedPaymentDoesNotRecordLedgerEntry() {
        when(settlementRepository.findByIdempotencyKey("KEY-999")).thenReturn(Optional.empty());
        when(mockPaymentGatewayClient.processPayment(any(), any(), any()))
                .thenReturn(new PaymentResult(false, null, "FAILED", "Payment timeout"));
        when(settlementRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        SettlementResponse response = settlementService.recordSettlement(groupId, request, fromUserId);

        assertNotNull(response);
        assertEquals("FAILED", response.getStatus());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Race condition handling: DB constraint violation returns existing settlement gracefully")
    void testDataIntegrityViolationReturnsExistingSettlement() {
        when(settlementRepository.findByIdempotencyKey("KEY-999"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new Settlement()));

        when(mockPaymentGatewayClient.processPayment(any(), any(), any()))
                .thenReturn(new PaymentResult(true, "TXN-123", "COMPLETED", null));

        when(settlementRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate key error"));

        SettlementResponse response = settlementService.recordSettlement(groupId, request, fromUserId);

        assertNotNull(response);
        verify(settlementRepository, times(2)).findByIdempotencyKey("KEY-999");
    }
}
