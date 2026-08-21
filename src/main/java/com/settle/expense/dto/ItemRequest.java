package com.settle.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemRequest {

    @NotBlank(message = "Item name is required")
    private String itemName;

    @DecimalMin(value = "0.01", message = "Item amount must be at least 0.01")
    private BigDecimal amount;

    @NotEmpty(message = "Item participant user IDs list cannot be empty")
    private Set<UUID> participantUserIds;
}
