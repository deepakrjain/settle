package com.settle.expense;

import com.settle.expense.dto.CreateExpenseRequest;
import com.settle.expense.dto.ExpenseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@Tag(name = "Expense Engine", description = "Endpoints for logging expenses with strategy pattern splits and paginated history retrieval")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    private UUID getCurrentUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Operation(summary = "Log an expense", description = "Creates an expense using EQUAL, PERCENTAGE, EXACT, SHARES, or ITEMIZED calculation strategy and generates ledger entries atomically")
    @ApiResponse(responseCode = "201", description = "Expense logged successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payload or strategy validation failure")
    @ApiResponse(responseCode = "403", description = "Forbidden if requester, payer, or participant is not a group member")
    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@PathVariable UUID groupId,
                                                         @Valid @RequestBody CreateExpenseRequest request) {
        UUID requestingUserId = getCurrentUserId();
        ExpenseResponse response = expenseService.createExpense(groupId, request, requestingUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get group expenses", description = "Retrieves paginated expense history for a group ordered by creation date descending")
    @ApiResponse(responseCode = "200", description = "Paginated expenses retrieved")
    @ApiResponse(responseCode = "403", description = "Forbidden if requesting user is not a group member")
    @GetMapping
    public ResponseEntity<Page<ExpenseResponse>> getGroupExpenses(
            @PathVariable UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UUID requestingUserId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<ExpenseResponse> response = expenseService.getGroupExpenses(groupId, pageable, requestingUserId);
        return ResponseEntity.ok(response);
    }
}
