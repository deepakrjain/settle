package com.settle.ledger;

import org.slf.Logger;
import org.slf.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

@Component
public class MockPaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGatewayClient.class);

    private final int failureRatePercent;
    private final Random random = new Random();

    public MockPaymentGatewayClient(
            @Value("${payment.gateway.failure-rate-percent:20}") int failureRatePercent) {
        this.failureRatePercent = failureRatePercent;
    }

    /**
     * Process a settlement payment via mock gateway.
     * Retries up to 3 times on PaymentGatewayException.
     *
     * Backoff multiplier = 2 with initial delay 500ms:
     * - Attempt 1 (t = 0ms): initial execution
     * - Attempt 2 (t = 500ms): 1st retry after 500ms delay
     * - Attempt 3 (t = 1500ms): 2nd retry after 500 * 2 = 1000ms delay
     * If Attempt 3 fails, @Recover method is triggered.
     */
    @Retryable(
            retryFor = PaymentGatewayException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2)
    )
    public PaymentResult processPayment(UUID fromUserId, UUID toUserId, BigDecimal amount) {
        log.info("[PaymentGateway] Attempting payment of {} from user {} to user {}...", amount, fromUserId, toUserId);

        int roll = random.nextInt(100) + 1;
        if (roll <= failureRatePercent) {
            log.warn("[PaymentGateway] SIMULATED FAILURE (roll {} <= failureRate {}%). Throwing PaymentGatewayException...",
                    roll, failureRatePercent);
            throw new PaymentGatewayException("Payment gateway temporary network timeout (simulated)");
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[PaymentGateway] SUCCESS! Payment processed successfully with transaction ID: {}", transactionId);

        return new PaymentResult(true, transactionId, "COMPLETED", null);
    }

    /**
     * Fallback method executed when all 3 retry attempts fail.
     */
    @Recover
    public PaymentResult recover(PaymentGatewayException ex, UUID fromUserId, UUID toUserId, BigDecimal amount) {
        log.error("[PaymentGateway] EXHAUSTED ALL RETRIES! Marking payment as permanently FAILED for user {} to user {}. Reason: {}",
                fromUserId, toUserId, ex.getMessage());

        return new PaymentResult(false, null, "FAILED", ex.getMessage());
    }
}
