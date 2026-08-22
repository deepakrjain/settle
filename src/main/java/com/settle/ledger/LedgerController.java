package com.settle.ledger;

import com.settle.ledger.dto.SettlementPlanResponse;
import com.settle.ledger.dto.UserBalance;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
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
}
