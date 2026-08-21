package com.settle.expense.dto;

import com.settle.expense.Expense;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExpenseResponse {
    private UUID id;
    private UUID groupId;
    private UUID paidByUserId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String category;
    private LocalDateTime createdAt;
    private List<ExpenseSplitResponse> splits;

    public static ExpenseResponse fromEntity(Expense expense) {
        List<ExpenseSplitResponse> splitResponses = expense.getSplits() != null
                ? expense.getSplits().stream().map(ExpenseSplitResponse::fromEntity).collect(Collectors.toList())
                : List.of();

        return new ExpenseResponse(
            expense.getId(),
            expense.getGroupId(),
            expense.getPaidByUserId(),
            expense.getAmount(),
            expense.getCurrency(),
            expense.getDescription(),
            expense.getCategory(),
            expense.getCreatedAt(),
            splitResponses
        );
    }
}
