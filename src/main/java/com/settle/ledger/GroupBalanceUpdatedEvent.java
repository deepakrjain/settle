package com.settle.ledger;

import java.util.UUID;

public class GroupBalanceUpdatedEvent {
    private final UUID groupId;

    public GroupBalanceUpdatedEvent(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getGroupId() {
        return groupId;
    }
}
