package com.settle.ledger;

import com.settle.ledger.dto.UserBalance;
import org.slf.Logger;
import org.slf.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
public class GroupBalanceEventListener {

    private static final Logger log = LoggerFactory.getLogger(GroupBalanceEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final LedgerService ledgerService;

    public GroupBalanceEventListener(SimpMessagingTemplate messagingTemplate,
                                     LedgerService ledgerService) {
        this.messagingTemplate = messagingTemplate;
        this.ledgerService = ledgerService;
    }

    /**
     * Listens for GroupBalanceUpdatedEvent AFTER the enclosing transaction commits successfully.
     * This prevents publishing phantom updates if the transaction rolls back.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleGroupBalanceUpdated(GroupBalanceUpdatedEvent event) {
        log.info("[WebSocket] Transaction committed successfully for group {}. Publishing balance update to /topic/groups/{}/balances...",
                event.getGroupId(), event.getGroupId());

        List<UserBalance> updatedBalances = ledgerService.getNetBalancesSystem(event.getGroupId());

        String destination = "/topic/groups/" + event.getGroupId() + "/balances";
        messagingTemplate.convertAndSend(destination, updatedBalances);
    }
}
