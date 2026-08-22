package com.settle.ledger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResult {
    private boolean successful;
    private String transactionId;
    private String status; // COMPLETED or FAILED
    private String errorMessage;
}
