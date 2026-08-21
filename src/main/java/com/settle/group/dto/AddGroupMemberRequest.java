package com.settle.group.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class AddGroupMemberRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;
}
