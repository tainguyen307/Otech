package com.vn.otech.group.controller;

import com.vn.otech.entity.GroupPostStatus;
import com.vn.otech.group.dto.GroupJoinDecisionRequest;
import com.vn.otech.group.dto.GroupJoinRequestResponse;
import com.vn.otech.group.dto.GroupMemberRoleRequest;
import com.vn.otech.group.dto.GroupPageResponse;
import com.vn.otech.group.dto.GroupPostRequest;
import com.vn.otech.group.dto.GroupPostResponse;
import com.vn.otech.group.dto.GroupRequest;
import com.vn.otech.group.dto.GroupResponse;
import com.vn.otech.group.service.GroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public GroupPageResponse list(
            @RequestParam(required = false) @Size(max = 120) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(48) int size) {
        return groupService.listGroups(search, categoryId, page, size);
    }

    @GetMapping("/mine")
    public GroupPageResponse mine(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(48) int size) {
        return groupService.listOwnedGroups(principal.getUsername(), page, size);
    }

    @PostMapping
    public ResponseEntity<GroupResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody GroupRequest request) {
        GroupResponse group = groupService.createGroup(principal.getUsername(), request);
        return ResponseEntity.created(URI.create("/api/groups/" + group.id())).body(group);
    }

    @GetMapping("/{groupId}")
    public GroupResponse detail(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID groupId) {
        return groupService.getGroup(groupId, principal == null ? null : principal.getUsername());
    }

    @PutMapping("/{groupId}")
    public GroupResponse update(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupRequest request) {
        return groupService.updateGroup(principal.getUsername(), groupId, request);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID groupId) {
        groupService.deleteGroup(principal.getUsername(), groupId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{groupId}/membership")
    public ResponseEntity<String> join(
            @AuthenticationPrincipal UserDetails principal, @PathVariable UUID groupId) {
        return ResponseEntity.status(HttpStatus.OK).body(groupService.joinGroup(principal.getUsername(), groupId));
    }

    @DeleteMapping("/{groupId}/membership")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal UserDetails principal, @PathVariable UUID groupId) {
        groupService.leaveGroup(principal.getUsername(), groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/join-requests")
    public List<GroupJoinRequestResponse> joinRequests(
            @AuthenticationPrincipal UserDetails principal, @PathVariable UUID groupId) {
        return groupService.listJoinRequests(principal.getUsername(), groupId);
    }

    @PutMapping("/{groupId}/join-requests/{requestId}")
    public ResponseEntity<Void> decideJoinRequest(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID groupId,
            @PathVariable UUID requestId,
            @Valid @RequestBody GroupJoinDecisionRequest request) {
        groupService.decideJoinRequest(
                principal.getUsername(), groupId, requestId, request.approved());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Void> updateMemberRole(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            @Valid @RequestBody GroupMemberRoleRequest request) {
        groupService.updateMemberRole(principal.getUsername(), groupId, memberId, request.role());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/posts")
    public List<GroupPostResponse> posts(
            @AuthenticationPrincipal UserDetails principal, @PathVariable UUID groupId) {
        return groupService.listPosts(principal == null ? null : principal.getUsername(), groupId);
    }

    @GetMapping("/{groupId}/posts/pending")
    public List<GroupPostResponse> pendingPosts(
            @AuthenticationPrincipal UserDetails principal, @PathVariable UUID groupId) {
        return groupService.listPendingPosts(principal.getUsername(), groupId);
    }

    @PostMapping("/{groupId}/posts")
    public ResponseEntity<GroupPostResponse> createPost(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID groupId,
            @Valid @RequestBody GroupPostRequest request) {
        GroupPostResponse post = groupService.createPost(principal.getUsername(), groupId, request);
        return ResponseEntity.created(URI.create("/api/groups/" + groupId + "/posts/" + post.id())).body(post);
    }

    @PutMapping("/{groupId}/posts/{postId}")
    public GroupPostResponse updatePost(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @Valid @RequestBody GroupPostRequest request) {
        return groupService.updatePost(principal.getUsername(), groupId, postId, request);
    }

    @DeleteMapping("/{groupId}/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID groupId,
            @PathVariable UUID postId) {
        groupService.deletePost(principal.getUsername(), groupId, postId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{groupId}/posts/{postId}/moderation")
    public GroupPostResponse reviewPost(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID groupId,
            @PathVariable UUID postId,
            @RequestParam GroupPostStatus status) {
        return groupService.reviewPost(principal.getUsername(), groupId, postId, status);
    }
}
