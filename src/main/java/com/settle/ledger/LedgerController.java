package com.settle.ledger;

import com.settle.ledger.dto.RecordSettlementRequest;
import com.settle.ledger.dto.SettlementPlanResponse;
import com.settle.ledger.dto.SettlementResponse;
import com.settle.ledger.dto.UserBalance;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}")
@Tag(name = "Ledger & Debt Settlement", description = "Endpoints for net balances, debt simplification algorithms, and idempotent payment recording")
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

    @Operation(summary = "Get group net balances", description = "Aggregates append-only ledger entries to calculate net balances for all group members (positive = net creditor, negative = net debtor)")
    @ApiResponse(responseCode = "200", description = "Net balances retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden if requesting user is not a group member")
    @GetMapping("/balances")
    public ResponseEntity<List<UserBalance>> getGroupBalances(@PathVariable UUID groupId) {
        UUID requestingUserId = getCurrentUserId();
        List<UserBalance> balances = ledgerService.getNetBalances(groupId, requestingUserId);
        return ResponseEntity.ok(balances);
    }

    @Operation(summary = "Get debt settlement plan", description = "Calculates Greedy O(N log N) transaction minimization plan, and exact Optimal O(2^N) subset-sum plan when non-zero member count is under 10")
    @ApiResponse(responseCode = "200", description = "Settlement plan generated successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden if requesting user is not a group member")
    @GetMapping("/settlement-plan")
    public ResponseEntity<SettlementPlanResponse> getSettlementPlan(@PathVariable UUID groupId) {
        UUID requestingUserId = getCurrentUserId();
        SettlementPlanResponse response = ledgerService.getSettlementPlan(groupId, requestingUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Record debt settlement payment", description = "Records a payment between group members idempotently with DB unique constraint protection and payment gateway retries")
    @ApiResponse(responseCode = "201", description = "Settlement recorded successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    @ApiResponse(responseCode = "403", description = "Forbidden if requester, payer, or recipient is not a group member")
    @PostMapping("/settlements")
    public ResponseEntity<SettlementResponse> recordSettlement(@PathVariable UUID groupId,
                                                               @Valid @RequestBody RecordSettlementRequest request) {
        UUID requestingUserId = getCurrentUserId();
        SettlementResponse response = settlementService.recordSettlement(groupId, request, requestingUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
