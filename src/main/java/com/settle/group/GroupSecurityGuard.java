package com.settle.group;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GroupSecurityGuard {

    private final GroupMemberRepository groupMemberRepository;

    public GroupSecurityGuard(GroupMemberRepository groupMemberRepository) {
        this.groupMemberRepository = groupMemberRepository;
    }

    /**
     * Checks if the specified user is a member of the group.
     * Throws an AccessDeniedException (which maps to 403 Forbidden) if not.
     */
    public void checkMembership(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new AccessDeniedException("User is not a member of this group");
        }
    }

    public boolean isMember(UUID groupId, UUID userId) {
        return groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
    }
}
