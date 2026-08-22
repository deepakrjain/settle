package com.settle.group;

import com.settle.group.dto.AddGroupMemberRequest;
import com.settle.group.dto.CreateGroupRequest;
import com.settle.group.dto.GroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@Tag(name = "Group Management", description = "Endpoints for creating groups, managing rosters, and group authorization")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    private UUID getCurrentUserId() {
        return (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Operation(summary = "Create a new group", description = "Creates a group and automatically attaches creator as first member atomically")
    @ApiResponse(responseCode = "201", description = "Group created successfully")
    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        UUID userId = getCurrentUserId();
        GroupResponse response = groupService.createGroup(request.getName(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Add a member to group", description = "Adds a user to group roster (requires requesting user to be an existing member)")
    @ApiResponse(responseCode = "200", description = "Member added successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden if requesting user is not a group member")
    @PostMapping("/{id}/members")
    public ResponseEntity<GroupResponse> addMember(@PathVariable UUID id,
                                                   @Valid @RequestBody AddGroupMemberRequest request) {
        UUID requestingUserId = getCurrentUserId();
        GroupResponse response = groupService.addMember(id, request.getUserId(), requestingUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get group details", description = "Returns group details and member roster (requires membership)")
    @ApiResponse(responseCode = "200", description = "Group details retrieved")
    @ApiResponse(responseCode = "403", description = "Forbidden if requesting user is not a group member")
    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable UUID id) {
        UUID requestingUserId = getCurrentUserId();
        GroupResponse response = groupService.getGroupDetails(id, requestingUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List user groups", description = "Returns all groups the authenticated user belongs to")
    @ApiResponse(responseCode = "200", description = "User groups retrieved")
    @GetMapping
    public ResponseEntity<List<GroupResponse>> getMyGroups() {
        UUID requestingUserId = getCurrentUserId();
        List<GroupResponse> responses = groupService.getUserGroups(requestingUserId);
        return ResponseEntity.ok(responses);
    }
}
