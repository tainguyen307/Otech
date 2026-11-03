package com.vn.otech.group.service;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Category;
import com.vn.otech.entity.Group;
import com.vn.otech.entity.GroupJoinRequest;
import com.vn.otech.entity.GroupMember;
import com.vn.otech.entity.GroupMemberId;
import com.vn.otech.entity.GroupPost;
import com.vn.otech.entity.GroupPostImage;
import com.vn.otech.entity.GroupPostStatus;
import com.vn.otech.entity.GroupVisibility;
import com.vn.otech.entity.RoleName;
import com.vn.otech.entity.User;
import com.vn.otech.group.dto.GroupJoinRequestResponse;
import com.vn.otech.group.dto.GroupPageResponse;
import com.vn.otech.group.dto.GroupPostResponse;
import com.vn.otech.group.dto.GroupRequest;
import com.vn.otech.group.dto.GroupResponse;
import com.vn.otech.group.repository.GroupJoinRequestRepository;
import com.vn.otech.group.repository.GroupMemberRepository;
import com.vn.otech.group.repository.GroupPostImageRepository;
import com.vn.otech.group.repository.GroupPostRepository;
import com.vn.otech.group.repository.GroupProjection;
import com.vn.otech.group.repository.GroupRepository;
import com.vn.otech.market.repository.CategoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class GroupService {
    private static final int MAX_PAGE_SIZE = 48;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupJoinRequestRepository joinRequestRepository;
    private final GroupPostRepository postRepository;
    private final GroupPostImageRepository postImageRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public GroupService(
            GroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            GroupJoinRequestRepository joinRequestRepository,
            GroupPostRepository postRepository,
            GroupPostImageRepository postImageRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public GroupPageResponse listGroups(String search, UUID categoryId, int page, int size) {
        validatePage(page, size);
        String normalizedSearch = search == null ? "" : search.trim();
        var result = groupRepository.findDiscoverableGroups(
                normalizedSearch, categoryId, PageRequest.of(page, size, Sort.unsorted()));
        return GroupPageResponse.from(result.map(this::toResponse));
    }

    public GroupPageResponse listOwnedGroups(String email, int page, int size) {
        validatePage(page, size);
        User owner = findActiveUser(email);
        var result = groupRepository.findOwnedGroups(
                owner.getId(), PageRequest.of(page, size, Sort.unsorted()));
        return GroupPageResponse.from(result.map(this::toResponse));
    }

    public GroupResponse getGroup(UUID groupId, String email) {
        Group group = findGroup(groupId);
        User viewer = findOptionalActiveUser(email);
        GroupMember member = viewer == null ? null : memberRepository
                .findById_GroupIdAndId_UserId(groupId, viewer.getId()).orElse(null);
        if (group.isHidden()) {
            throw groupNotFound();
        }
        String requestStatus = viewer == null ? null : joinRequestRepository
                .findByGroup_IdAndUser_IdAndStatus(groupId, viewer.getId(), "PENDING")
                .map(GroupJoinRequest::getStatus)
                .orElse(null);
        return toResponse(
                group,
                ownerOf(group, viewer) ? "OWNER" : member == null ? null : member.getRole(),
                ownerOf(group, viewer) || member != null,
                requestStatus);
    }

    @Transactional
    public GroupResponse createGroup(String email, GroupRequest request) {
        User owner = findActiveUser(email);
        Group group = new Group();
        group.setOwner(owner);
        applyRequest(group, request, true);
        group = groupRepository.saveAndFlush(group);
        GroupMember ownerMembership = new GroupMember();
        ownerMembership.setId(new GroupMemberId(group.getId(), owner.getId()));
        ownerMembership.setGroup(group);
        ownerMembership.setUser(owner);
        ownerMembership.setRole("OWNER");
        memberRepository.save(ownerMembership);
        return toResponse(group, "OWNER", true, null);
    }

    @Transactional
    public GroupResponse updateGroup(String email, UUID groupId, GroupRequest request) {
        User user = findActiveUser(email);
        Group group = findGroup(groupId);
        if (!ownerOf(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group owner can update this group");
        }
        applyRequest(group, request, false);
        return toResponse(groupRepository.save(group), "OWNER", true, null);
    }

    @Transactional
    public void deleteGroup(String email, UUID groupId) {
        User user = findActiveUser(email);
        Group group = findGroup(groupId);
        if (!ownerOf(group, user) && !isAdministrator(user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group owner can delete this group");
        }
        joinRequestRepository.deleteByGroup_Id(groupId);
        postRepository.deleteByGroup_Id(groupId);
        groupRepository.delete(group);
    }

    @Transactional
    public String joinGroup(String email, UUID groupId) {
        User user = findActiveUser(email);
        Group group = requireVisibleGroup(groupId);
        if (ownerOf(group, user)) {
            return "JOINED";
        }
        if (memberRepository.existsById_GroupIdAndId_UserId(groupId, user.getId())) {
            return "JOINED";
        }
        if (group.getVisibility() == GroupVisibility.PUBLIC) {
            saveMembership(group, user, "MEMBER");
            return "JOINED";
        }
        if (!joinRequestRepository.existsByGroup_IdAndUser_IdAndStatus(groupId, user.getId(), "PENDING")) {
            GroupJoinRequest request = new GroupJoinRequest();
            request.setGroup(group);
            request.setUser(user);
            request.setStatus("PENDING");
            joinRequestRepository.save(request);
        }
        return "PENDING";
    }

    @Transactional
    public void leaveGroup(String email, UUID groupId) {
        User user = findActiveUser(email);
        Group group = findGroup(groupId);
        if (ownerOf(group, user)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The group owner cannot leave their own group");
        }
        memberRepository.findById_GroupIdAndId_UserId(groupId, user.getId())
                .ifPresent(memberRepository::delete);
        joinRequestRepository.findByGroup_IdAndUser_IdAndStatus(groupId, user.getId(), "PENDING")
                .ifPresent(joinRequestRepository::delete);
    }

    public List<GroupJoinRequestResponse> listJoinRequests(String email, UUID groupId) {
        Group group = requireVisibleGroup(groupId);
        User viewer = findActiveUser(email);
        requireModerator(group, viewer);
        return joinRequestRepository.findByGroup_IdAndStatusOrderById(groupId, "PENDING").stream()
                .map(request -> new GroupJoinRequestResponse(
                        request.getId(),
                        request.getUser().getId(),
                        request.getUser().getFullName(),
                        request.getUser().getAvatarUrl(),
                        request.getStatus()))
                .toList();
    }

    @Transactional
    public void decideJoinRequest(String email, UUID groupId, UUID requestId, boolean approved) {
        Group group = requireVisibleGroup(groupId);
        User moderator = findActiveUser(email);
        requireModerator(group, moderator);
        GroupJoinRequest request = joinRequestRepository.findByIdAndGroup_Id(requestId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Join request not found"));
        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Join request has already been reviewed");
        }
        request.setStatus(approved ? "APPROVED" : "REJECTED");
        if (approved) {
            saveMembership(group, request.getUser(), "MEMBER");
        }
    }

    @Transactional
    public void updateMemberRole(String email, UUID groupId, UUID memberId, String role) {
        Group group = requireVisibleGroup(groupId);
        User owner = findActiveUser(email);
        if (!ownerOf(group, owner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group owner can manage moderators");
        }
        GroupMember member = memberRepository.findById_GroupIdAndId_UserId(groupId, memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group member not found"));
        if ("OWNER".equals(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The group owner role cannot be changed");
        }
        member.setRole(role);
    }

    public List<GroupPostResponse> listPosts(String email, UUID groupId) {
        Group group = requireVisibleGroup(groupId);
        User viewer = findOptionalActiveUser(email);
        GroupMember member = viewer == null ? null : memberRepository
                .findById_GroupIdAndId_UserId(groupId, viewer.getId()).orElse(null);
        if (group.getVisibility() == GroupVisibility.PRIVATE && member == null && !ownerOf(group, viewer)) {
            throw groupNotFound();
        }
        boolean moderator = viewer != null && (isAdministrator(viewer) || isModerator(group, viewer));
        return postRepository.findByGroup_IdOrderByCreatedAtDesc(groupId).stream()
                .filter(post -> post.getStatus() == GroupPostStatus.APPROVED
                        || (viewer != null && post.getAuthor().getId().equals(viewer.getId()))
                        || (moderator && post.getStatus() == GroupPostStatus.PENDING))
                .map(this::toPostResponse)
                .toList();
    }

    public List<GroupPostResponse> listPendingPosts(String email, UUID groupId) {
        Group group = requireVisibleGroup(groupId);
        requireModerator(group, findActiveUser(email));
        return postRepository.findByGroup_IdAndStatusOrderByCreatedAtDesc(groupId, GroupPostStatus.PENDING).stream()
                .map(this::toPostResponse)
                .toList();
    }

    @Transactional
    public GroupPostResponse createPost(
            String email, UUID groupId, com.vn.otech.group.dto.GroupPostRequest request) {
        User author = findActiveUser(email);
        Group group = requireVisibleGroup(groupId);
        requireMembership(group, author);
        GroupPost post = new GroupPost();
        post.setGroup(group);
        post.setAuthor(author);
        post.setContent(request.content().trim());
        post.setStatus(shouldRequireApproval(group, author) ? GroupPostStatus.PENDING : GroupPostStatus.APPROVED);
        post = postRepository.saveAndFlush(post);
        savePostImage(post, request.imageUrl());
        return toPostResponse(post);
    }

    @Transactional
    public GroupPostResponse updatePost(
            String email, UUID groupId, UUID postId, com.vn.otech.group.dto.GroupPostRequest request) {
        User editor = findActiveUser(email);
        Group group = requireVisibleGroup(groupId);
        GroupPost post = findPost(groupId, postId);
        boolean moderator = isAdministrator(editor) || isModerator(group, editor);
        if (!post.getAuthor().getId().equals(editor.getId()) && !moderator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot edit this group post");
        }
        post.setContent(request.content().trim());
        post.setStatus(shouldRequireApproval(group, editor) ? GroupPostStatus.PENDING : GroupPostStatus.APPROVED);
        postImageRepository.deleteByGroupPost_Id(postId);
        savePostImage(post, request.imageUrl());
        return toPostResponse(postRepository.save(post));
    }

    @Transactional
    public void deletePost(String email, UUID groupId, UUID postId) {
        User user = findActiveUser(email);
        Group group = requireVisibleGroup(groupId);
        GroupPost post = findPost(groupId, postId);
        if (!post.getAuthor().getId().equals(user.getId())
                && !isAdministrator(user) && !isModerator(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete this group post");
        }
        postRepository.delete(post);
    }

    @Transactional
    public GroupPostResponse reviewPost(String email, UUID groupId, UUID postId, GroupPostStatus decision) {
        Group group = requireVisibleGroup(groupId);
        User moderator = findActiveUser(email);
        requireModerator(group, moderator);
        if (decision != GroupPostStatus.APPROVED && decision != GroupPostStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review decision must be APPROVED or REJECTED");
        }
        GroupPost post = findPost(groupId, postId);
        if (post.getStatus() != GroupPostStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending posts can be reviewed");
        }
        post.setStatus(decision);
        return toPostResponse(postRepository.save(post));
    }

    private void applyRequest(Group group, GroupRequest request, boolean creating) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        group.setName(request.name().trim());
        group.setDescription(request.description() == null ? null : request.description().trim());
        group.setCategory(category);
        group.setVisibility(request.visibility());
        group.setAvatarUrl(request.avatarUrl());
        group.setCoverUrl(request.coverUrl());
        if (request.requirePostApproval() != null) {
            group.setRequirePostApproval(request.requirePostApproval());
        } else if (creating) {
            group.setRequirePostApproval(true);
        }
    }

    private GroupResponse toResponse(GroupProjection group) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getAvatarUrl(),
                group.getCoverUrl(),
                group.getCategoryId(),
                group.getCategoryName(),
                GroupVisibility.valueOf(group.getVisibility()),
                Boolean.TRUE.equals(group.getRequirePostApproval()),
                group.getOwnerName(),
                valueOrZero(group.getMemberCount()),
                valueOrZero(group.getPostCount()),
                null,
                false,
                null,
                group.getCreatedAt());
    }

    private GroupResponse toResponse(Group group, String role, boolean member, String requestStatus) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getAvatarUrl(),
                group.getCoverUrl(),
                group.getCategory() == null ? null : group.getCategory().getId(),
                group.getCategory() == null ? null : group.getCategory().getName(),
                group.getVisibility(),
                group.isRequirePostApproval(),
                group.getOwner() == null ? "Otech member" : group.getOwner().getFullName(),
                memberRepository.countById_GroupId(group.getId())
                        + (group.getOwner() != null
                                && !memberRepository.existsById_GroupIdAndId_UserId(
                                        group.getId(), group.getOwner().getId()) ? 1 : 0),
                postRepository.countByGroup_IdAndStatus(group.getId(), GroupPostStatus.APPROVED),
                role,
                member,
                requestStatus,
                group.getCreatedAt());
    }

    private GroupPostResponse toPostResponse(GroupPost post) {
        List<String> imageUrls = postImageRepository.findByGroupPost_IdOrderById(post.getId()).stream()
                .map(GroupPostImage::getImageUrl)
                .toList();
        return new GroupPostResponse(
                post.getId(),
                post.getGroup().getId(),
                post.getAuthor().getId(),
                post.getAuthor().getFullName(),
                post.getAuthor().getAvatarUrl(),
                post.getContent(),
                imageUrls,
                post.getStatus(),
                post.getCreatedAt());
    }

    private void savePostImage(GroupPost post, String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            GroupPostImage image = new GroupPostImage();
            image.setGroupPost(post);
            image.setImageUrl(imageUrl.trim());
            postImageRepository.save(image);
        }
    }

    private void saveMembership(Group group, User user, String role) {
        GroupMember member = new GroupMember();
        member.setId(new GroupMemberId(group.getId(), user.getId()));
        member.setGroup(group);
        member.setUser(user);
        member.setRole(role);
        memberRepository.save(member);
    }

    private boolean shouldRequireApproval(Group group, User author) {
        return group.isRequirePostApproval() && !ownerOf(group, author) && !isModerator(group, author);
    }

    private void requireMembership(Group group, User user) {
        if (!ownerOf(group, user)
                && !memberRepository.existsById_GroupIdAndId_UserId(group.getId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Join the group before posting");
        }
    }

    private void requireModerator(Group group, User user) {
        if (!isAdministrator(user) && !isModerator(group, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Group moderator access required");
        }
    }

    private boolean isModerator(Group group, User user) {
        if (ownerOf(group, user)) {
            return true;
        }
        return memberRepository.findById_GroupIdAndId_UserId(group.getId(), user.getId())
                .map(member -> "OWNER".equals(member.getRole()) || "MODERATOR".equals(member.getRole()))
                .orElse(false);
    }

    private boolean isAdministrator(User user) {
        return user.getRole() != null && user.getRole().getName() == RoleName.ADMIN;
    }

    private boolean ownerOf(Group group, User user) {
        return user != null && group.getOwner() != null && group.getOwner().getId().equals(user.getId());
    }

    private Group requireVisibleGroup(UUID groupId) {
        Group group = findGroup(groupId);
        if (group.isHidden()) {
            throw groupNotFound();
        }
        return group;
    }

    private Group findGroup(UUID groupId) {
        return groupRepository.findById(groupId).orElseThrow(GroupService::groupNotFound);
    }

    private GroupPost findPost(UUID groupId, UUID postId) {
        return postRepository.findByIdAndGroup_Id(postId, groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group post not found"));
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }
        return user;
    }

    private User findOptionalActiveUser(String email) {
        if (email == null) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(email)
                .filter(user -> user.getStatus() == AccountStatus.ACTIVE)
                .orElse(null);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid page or page size");
        }
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    private static ResponseStatusException groupNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
    }
}
