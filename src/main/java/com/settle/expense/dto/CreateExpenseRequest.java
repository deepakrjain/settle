package com.settle.expense.dto;

import com.settle.expense.SplitType;
import com.settle.expense.validation.ValidSplitData;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@ValidSplitData
public class CreateExpenseRequest {

    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency = "INR";

    @NotBlank(message = "Description is required")
    private String description;

    private String category = "GENERAL";

    @NotNull(message = "PaidByUserId is required")
    private UUID paidByUserId;

    @NotNull(message = "SplitType is required")
    private SplitType splitType = SplitType.EQUAL;

    // Type-specific data
    private Set<UUID> participantUserIds;             // For EQUAL
    private Map<UUID, BigDecimal> percentages;        // For PERCENTAGE
    private Map<UUID, BigDecimal> exactAmounts;       // For EXACT
    private Map<UUID, Integer> shares;                // For SHARES
    private List<ItemRequest> items;                 // For ITEMIZED
    private BigDecimal taxAndTip;                     // For ITEMIZED (optional)
}
