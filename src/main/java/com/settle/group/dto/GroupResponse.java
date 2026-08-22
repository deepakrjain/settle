package com.settle.group.dto;

import com.settle.group.Group;
import com.settle.group.GroupMember;
import com.settle.user.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupResponse {
    private UUID id;
    private String name;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private List<GroupMemberResponse> members;

    public static GroupResponse fromEntity(Group group, List<GroupMember> members, Map<UUID, User> userMap) {
        List<GroupMemberResponse> memberResponses = members != null
                ? members.stream()
                        .map(m -> GroupMemberResponse.fromEntity(m, userMap != null ? userMap.get(m.getUserId()) : null))
                        .collect(Collectors.toList())
                : List.of();

        return new GroupResponse(
            group.getId(),
            group.getName(),
            group.getCreatedBy(),
            group.getCreatedAt(),
            memberResponses
        );
    }

    public static GroupResponse fromEntity(Group group, List<GroupMember> members) {
        return fromEntity(group, members, Map.of());
    }
}
