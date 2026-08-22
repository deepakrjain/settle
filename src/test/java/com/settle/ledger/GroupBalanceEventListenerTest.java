package com.settle.ledger;

import com.settle.ledger.dto.UserBalance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupBalanceEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private LedgerService ledgerService;

    @InjectMocks
    private GroupBalanceEventListener listener;

    @Test
    @DisplayName("Transaction commit listener broadcasts updated balances to /topic/groups/{groupId}/balances")
    void testHandleGroupBalanceUpdatedBroadcasting() {
        UUID groupId = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();

        List<UserBalance> mockBalances = List.of(new UserBalance(user1, new BigDecimal("50.00")));
        when(ledgerService.getNetBalancesSystem(groupId)).thenReturn(mockBalances);

        GroupBalanceUpdatedEvent event = new GroupBalanceUpdatedEvent(groupId);
        listener.handleGroupBalanceUpdated(event);

        verify(ledgerService, times(1)).getNetBalancesSystem(groupId);
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/groups/" + groupId + "/balances"),
                eq(mockBalances)
        );
    }
}
