package com.settle.group;

import com.settle.group.dto.AddGroupMemberRequest;
import com.settle.group.dto.CreateGroupRequest;
import com.settle.group.dto.GroupResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    private UUID getCurrentUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        UUID userId = getCurrentUserId();
        GroupResponse response = groupService.createGroup(request.getName(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupResponse> addMember(@PathVariable UUID id,
                                                   @Valid @RequestBody AddGroupMemberRequest request) {
        UUID requestingUserId = getCurrentUserId();
        GroupResponse response = groupService.addMember(id, request.getUserId(), requestingUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable UUID id) {
        UUID requestingUserId = getCurrentUserId();
        GroupResponse response = groupService.getGroupDetails(id, requestingUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getMyGroups() {
        UUID requestingUserId = getCurrentUserId();
        List<GroupResponse> responses = groupService.getUserGroups(requestingUserId);
        return ResponseEntity.ok(responses);
    }
}
