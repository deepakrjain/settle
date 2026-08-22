package com.settle.group.dto;

import com.settle.group.GroupMember;
import com.settle.user.User;
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
    private String userDisplayName;
    private String userEmail;
    private LocalDateTime joinedAt;

    public static GroupMemberResponse fromEntity(GroupMember member, User user) {
        return new GroupMemberResponse(
            member.getId(),
            member.getGroupId(),
            member.getUserId(),
            user != null ? user.getDisplayName() : null,
            user != null ? user.getEmail() : null,
            member.getJoinedAt()
        );
    }
}
