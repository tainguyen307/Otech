package com.vn.otech.group.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Category;
import com.vn.otech.entity.Group;
import com.vn.otech.entity.GroupPost;
import com.vn.otech.entity.GroupPostStatus;
import com.vn.otech.entity.GroupVisibility;
import com.vn.otech.entity.User;
import com.vn.otech.group.dto.GroupPostRequest;
import com.vn.otech.group.dto.GroupRequest;
import com.vn.otech.group.repository.GroupJoinRequestRepository;
import com.vn.otech.group.repository.GroupMemberRepository;
import com.vn.otech.group.repository.GroupPostImageRepository;
import com.vn.otech.group.repository.GroupPostRepository;
import com.vn.otech.group.repository.GroupProjection;
import com.vn.otech.group.repository.GroupRepository;
import com.vn.otech.market.repository.CategoryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository memberRepository;
    @Mock private GroupJoinRequestRepository joinRequestRepository;
    @Mock private GroupPostRepository postRepository;
    @Mock private GroupPostImageRepository postImageRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;

    @Test
    void listTrimsSearchAndReturnsCategoryAndPagingMetadata() {
        UUID categoryId = UUID.randomUUID();
        GroupProjection group = projection(UUID.randomUUID(), categoryId);
        when(groupRepository.findDiscoverableGroups(eq("books"), eq(categoryId), any()))
                .thenReturn(new PageImpl<>(List.of(group), PageRequest.of(0, 24), 1));

        var result = service().listGroups("  books  ", categoryId, 0, 24);

        assertEquals(group.getId(), result.items().getFirst().id());
        assertEquals("Books", result.items().getFirst().categoryName());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        verify(groupRepository).findDiscoverableGroups(eq("books"), eq(categoryId), any());
    }

    @Test
    void rejectsPageSizesAboveMaximum() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().listGroups(null, null, 0, 49));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void newGroupsRequirePostApprovalByDefaultAndAddOwnerAsMember() {
        UUID ownerId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        User owner = user(ownerId, "owner@example.com", "Owner");
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Books");
        when(userRepository.findByEmailIgnoreCase(owner.getEmail())).thenReturn(Optional.of(owner));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(groupRepository.saveAndFlush(any(Group.class))).thenAnswer(invocation -> {
            Group group = invocation.getArgument(0);
            group.setId(UUID.randomUUID());
            group.setCreatedAt(LocalDateTime.now());
            return group;
        });
        when(memberRepository.countById_GroupId(any())).thenReturn(1L);
        when(postRepository.countByGroup_IdAndStatus(any(), eq(GroupPostStatus.APPROVED))).thenReturn(0L);

        var result = service().createGroup(owner.getEmail(), new GroupRequest(
                "Local books", "Buy and sell used books", categoryId, GroupVisibility.PUBLIC, null, null, null));

        assertEquals("OWNER", result.viewerRole());
        assertTrue(result.member());
        assertTrue(result.requirePostApproval());
        assertEquals(categoryId, result.categoryId());
        verify(memberRepository).save(any());
    }

    @Test
    void memberPostsWaitForReviewWhenGroupRequiresApproval() {
        UUID authorId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        User author = user(authorId, "member@example.com", "Member");
        User owner = user(ownerId, "owner@example.com", "Owner");
        Group group = new Group();
        group.setId(groupId);
        group.setOwner(owner);
        group.setVisibility(GroupVisibility.PUBLIC);
        group.setRequirePostApproval(true);
        when(userRepository.findByEmailIgnoreCase(author.getEmail())).thenReturn(Optional.of(author));
        when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(memberRepository.existsById_GroupIdAndId_UserId(groupId, authorId)).thenReturn(true);
        when(postRepository.saveAndFlush(any(GroupPost.class))).thenAnswer(invocation -> {
            GroupPost post = invocation.getArgument(0);
            post.setId(UUID.randomUUID());
            post.setCreatedAt(LocalDateTime.now());
            return post;
        });
        when(postImageRepository.findByGroupPost_IdOrderById(any())).thenReturn(List.of());

        var result = service().createPost(author.getEmail(), groupId, new GroupPostRequest("A listing", null));

        assertEquals(GroupPostStatus.PENDING, result.status());
        assertEquals("A listing", result.content());
    }

    private GroupService service() {
        return new GroupService(
                groupRepository,
                memberRepository,
                joinRequestRepository,
                postRepository,
                postImageRepository,
                categoryRepository,
                userRepository);
    }

    private User user(UUID id, String email, String name) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName(name);
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }

    private GroupProjection projection(UUID id, UUID categoryId) {
        return new GroupProjection() {
            public UUID getId() { return id; }
            public String getName() { return "Book circle"; }
            public String getDescription() { return "A group for used books"; }
            public String getAvatarUrl() { return null; }
            public String getCoverUrl() { return null; }
            public UUID getCategoryId() { return categoryId; }
            public String getCategoryName() { return "Books"; }
            public String getVisibility() { return "PUBLIC"; }
            public Boolean getRequirePostApproval() { return true; }
            public String getOwnerName() { return "Owner"; }
            public Long getMemberCount() { return 3L; }
            public Long getPostCount() { return 2L; }
            public LocalDateTime getCreatedAt() { return LocalDateTime.now(); }
        };
    }
}
