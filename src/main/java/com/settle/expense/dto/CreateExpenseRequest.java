package com.settle.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Data
public class CreateExpenseRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency = "INR";

    @NotBlank(message = "Description is required")
    private String description;

    private String category = "GENERAL";

    @NotNull(message = "PaidByUserId is required")
    private UUID paidByUserId;

    @NotEmpty(message = "Participant user IDs list cannot be empty")
    private Set<UUID> participantUserIds;
}
