package com.settle.group;

import com.settle.group.dto.GroupResponse;
import com.settle.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupSecurityGuard groupSecurityGuard;

    public GroupService(GroupRepository groupRepository,
                        GroupMemberRepository groupMemberRepository,
                        UserRepository userRepository,
                        GroupSecurityGuard groupSecurityGuard) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.userRepository = userRepository;
        this.groupSecurityGuard = groupSecurityGuard;
    }

    /**
     * Creates a group and automatically adds the creator as the first member.
     * Annotated with @Transactional so if member creation fails, the group creation is rolled back.
     */
    @Transactional
    public GroupResponse createGroup(String name, UUID creatorUserId) {
        if (!userRepository.existsById(creatorUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        Group group = new Group();
        group.setName(name);
        group.setCreatedBy(creatorUserId);
        Group savedGroup = groupRepository.save(group);

        GroupMember creatorMember = new GroupMember();
        creatorMember.setGroupId(savedGroup.getId());
        creatorMember.setUserId(creatorUserId);
        GroupMember savedMember = groupMemberRepository.save(creatorMember);

        return GroupResponse.fromEntity(savedGroup, List.of(savedMember));
    }

    /**
     * Adds a new member to an existing group.
     * Only existing members of the group can add new members.
     */
    @Transactional
    public GroupResponse addMember(UUID groupId, UUID targetUserId, UUID requestingUserId) {
        // Enforce authorization check: requester must be a member
        groupSecurityGuard.checkMembership(groupId, requestingUserId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        if (!userRepository.existsById(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found");
        }

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, targetUserId)) {
            throw new IllegalArgumentException("User is already a member of this group");
        }

        GroupMember newMember = new GroupMember();
        newMember.setGroupId(groupId);
        newMember.setUserId(targetUserId);
        groupMemberRepository.save(newMember);

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        return GroupResponse.fromEntity(group, members);
    }

    /**
     * Retrieves details for a specific group. Requester must be a member.
     */
    @Transactional(readOnly = true)
    public GroupResponse getGroupDetails(UUID groupId, UUID requestingUserId) {
        groupSecurityGuard.checkMembership(groupId, requestingUserId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));

        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        return GroupResponse.fromEntity(group, members);
    }

    /**
     * Lists all groups that the requesting user belongs to.
     */
    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(UUID requestingUserId) {
        List<GroupMember> userMemberships = groupMemberRepository.findByUserId(requestingUserId);
        List<UUID> groupIds = userMemberships.stream()
                .map(GroupMember::getGroupId)
                .collect(Collectors.toList());

        List<Group> groups = groupRepository.findAllByIdIn(groupIds);

        return groups.stream().map(group -> {
            List<GroupMember> members = groupMemberRepository.findByGroupId(group.getId());
            return GroupResponse.fromEntity(group, members);
        }).collect(Collectors.toList());
    }
}
