package com.settle.group.dto;

import com.settle.group.Group;
import com.settle.group.GroupMember;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
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

    public static GroupResponse fromEntity(Group group, List<GroupMember> members) {
        List<GroupMemberResponse> memberResponses = members != null
                ? members.stream().map(GroupMemberResponse::fromEntity).collect(Collectors.toList())
                : List.of();

        return new GroupResponse(
            group.getId(),
            group.getName(),
            group.getCreatedBy(),
            group.getCreatedAt(),
            memberResponses
        );
    }
}
