package com.settle.group.dto;

import com.settle.group.GroupMember;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemberResponse {
    private UUID id;
    private UUID groupId;
    private UUID userId;
    private LocalDateTime joinedAt;

    public static GroupMemberResponse fromEntity(GroupMember member) {
        return new GroupMemberResponse(
            member.getId(),
            member.getGroupId(),
            member.getUserId(),
            member.getJoinedAt()
        );
    }
}
