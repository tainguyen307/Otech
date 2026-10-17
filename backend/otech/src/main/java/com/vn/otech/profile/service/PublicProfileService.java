package com.vn.otech.profile.service;

import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.User;
import com.vn.otech.entity.UserFollow;
import com.vn.otech.entity.UserFollowId;
import com.vn.otech.post.dto.PostResponse;
import com.vn.otech.post.service.PostService;
import com.vn.otech.profile.dto.PublicProfileResponse;
import com.vn.otech.profile.repository.UserFollowRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PublicProfileService {
    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final PostService postService;

    public PublicProfileService(UserRepository userRepository,
                                UserFollowRepository userFollowRepository,
                                PostService postService) {
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.postService = postService;
    }

    public PublicProfileResponse getProfile(UUID userId, String viewerEmail) {
        User profileUser = findActiveUser(userId);
        User viewer = viewerEmail == null ? null : findActiveUser(viewerEmail);
        List<PostResponse> posts = postService.listPostsByAuthor(userId);
        return new PublicProfileResponse(
                profileUser.getId(),
                profileUser.getFullName(),
                profileUser.getAvatarUrl(),
                profileUser.getBio(),
                profileUser.getCreatedAt(),
                posts.size(),
                userFollowRepository.countByIdFollowingId(userId),
                userFollowRepository.countByIdFollowerId(userId),
                viewer != null && !viewer.getId().equals(userId)
                        && userFollowRepository.existsByIdFollowerIdAndIdFollowingId(viewer.getId(), userId),
                posts);
    }

    @Transactional
    public void follow(String viewerEmail, UUID userId) {
        User viewer = findActiveUser(viewerEmail);
        User target = findActiveUser(userId);
        if (viewer.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot follow yourself");
        }
        if (!userFollowRepository.existsByIdFollowerIdAndIdFollowingId(viewer.getId(), target.getId())) {
            UserFollow follow = new UserFollow();
            follow.setId(new UserFollowId(viewer.getId(), target.getId()));
            follow.setFollower(viewer);
            follow.setFollowing(target);
            userFollowRepository.save(follow);
        }
    }

    @Transactional
    public void unfollow(String viewerEmail, UUID userId) {
        User viewer = findActiveUser(viewerEmail);
        User target = findActiveUser(userId);
        if (viewer.getId().equals(target.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot unfollow yourself");
        }
        userFollowRepository.deleteById(new UserFollowId(viewer.getId(), target.getId()));
    }

    private User findActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }
}
