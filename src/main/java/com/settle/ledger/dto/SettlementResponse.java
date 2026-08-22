package com.settle.ledger.dto;

import com.settle.ledger.Settlement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettlementResponse {
    private UUID id;
    private UUID groupId;
    private UUID fromUserId;
    private UUID toUserId;
    private BigDecimal amount;
    private String idempotencyKey;
    private String status;
    private LocalDateTime createdAt;

    public static SettlementResponse fromEntity(Settlement settlement) {
        return new SettlementResponse(
            settlement.getId(),
            settlement.getGroupId(),
            settlement.getFromUserId(),
            settlement.getToUserId(),
            settlement.getAmount(),
            settlement.getIdempotencyKey(),
            settlement.getStatus(),
            settlement.getCreatedAt()
        );
    }
}
