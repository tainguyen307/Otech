package com.vn.otech.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.User;
import com.vn.otech.entity.UserFollow;
import com.vn.otech.post.service.PostService;
import com.vn.otech.profile.dto.PublicProfileResponse;
import com.vn.otech.profile.repository.UserFollowRepository;
import com.vn.otech.profile.service.PublicProfileService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PublicProfileServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private UserFollowRepository userFollowRepository;
    @Mock private PostService postService;

    @Test
    void publicProfileIncludesPostAndFollowCountsWithoutExposingPrivateFields() {
        User profile = user(UUID.randomUUID(), "profile@example.com");
        User viewer = user(UUID.randomUUID(), "viewer@example.com");
        when(userRepository.findById(profile.getId())).thenReturn(Optional.of(profile));
        when(userRepository.findByEmailIgnoreCase(viewer.getEmail())).thenReturn(Optional.of(viewer));
        when(userFollowRepository.countByIdFollowingId(profile.getId())).thenReturn(7L);
        when(userFollowRepository.countByIdFollowerId(profile.getId())).thenReturn(3L);
        when(userFollowRepository.existsByIdFollowerIdAndIdFollowingId(viewer.getId(), profile.getId()))
                .thenReturn(true);
        when(postService.listPostsByAuthor(profile.getId())).thenReturn(java.util.List.of());

        PublicProfileResponse result = service().getProfile(profile.getId(), viewer.getEmail());

        assertEquals(profile.getId(), result.userId());
        assertEquals(0, result.postCount());
        assertEquals(7, result.followerCount());
        assertEquals(3, result.followingCount());
        org.junit.jupiter.api.Assertions.assertTrue(result.followedByCurrentUser());
    }

    @Test
    void followIsIdempotentAndCreatesTheJoinEntityOnce() {
        User viewer = user(UUID.randomUUID(), "viewer@example.com");
        User target = user(UUID.randomUUID(), "target@example.com");
        when(userRepository.findByEmailIgnoreCase(viewer.getEmail())).thenReturn(Optional.of(viewer));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userFollowRepository.existsByIdFollowerIdAndIdFollowingId(viewer.getId(), target.getId()))
                .thenReturn(false, true);

        service().follow(viewer.getEmail(), target.getId());
        service().follow(viewer.getEmail(), target.getId());

        verify(userFollowRepository).save(any(UserFollow.class));
    }

    @Test
    void userCannotFollowThemself() {
        User viewer = user(UUID.randomUUID(), "viewer@example.com");
        when(userRepository.findByEmailIgnoreCase(viewer.getEmail())).thenReturn(Optional.of(viewer));
        when(userRepository.findById(viewer.getId())).thenReturn(Optional.of(viewer));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().follow(viewer.getEmail(), viewer.getId()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userFollowRepository, never()).save(any(UserFollow.class));
    }

    @Test
    void inactiveProfilesAreNotPublic() {
        User inactive = user(UUID.randomUUID(), "inactive@example.com");
        inactive.setStatus(AccountStatus.LOCKED);
        when(userRepository.findById(inactive.getId())).thenReturn(Optional.of(inactive));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service().getProfile(inactive.getId(), null));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertFalse(exception.getReason().isBlank());
    }

    private PublicProfileService service() {
        return new PublicProfileService(userRepository, userFollowRepository, postService);
    }

    private User user(UUID id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFullName("Profile user");
        user.setStatus(AccountStatus.ACTIVE);
        return user;
    }
}
