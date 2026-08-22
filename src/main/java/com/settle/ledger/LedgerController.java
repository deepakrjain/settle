package com.settle.ledger;

import com.settle.ledger.dto.RecordSettlementRequest;
import com.settle.ledger.dto.SettlementPlanResponse;
import com.settle.ledger.dto.SettlementResponse;
import com.settle.ledger.dto.UserBalance;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class LedgerController {

    private final LedgerService ledgerService;
    private final SettlementService settlementService;

    public LedgerController(LedgerService ledgerService, SettlementService settlementService) {
        this.ledgerService = ledgerService;
        this.settlementService = settlementService;
    }

    private UUID getCurrentUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/balances")
    public ResponseEntity<List<UserBalance>> getGroupBalances(@PathVariable UUID groupId) {
        UUID requestingUserId = getCurrentUserId();
        List<UserBalance> balances = ledgerService.getNetBalances(groupId, requestingUserId);
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/settlement-plan")
    public ResponseEntity<SettlementPlanResponse> getSettlementPlan(@PathVariable UUID groupId) {
        UUID requestingUserId = getCurrentUserId();
        SettlementPlanResponse response = ledgerService.getSettlementPlan(groupId, requestingUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/settlements")
    public ResponseEntity<SettlementResponse> recordSettlement(@PathVariable UUID groupId,
                                                               @Valid @RequestBody RecordSettlementRequest request) {
        UUID requestingUserId = getCurrentUserId();
        SettlementResponse response = settlementService.recordSettlement(groupId, request, requestingUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
