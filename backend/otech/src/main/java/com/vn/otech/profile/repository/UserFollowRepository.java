package com.vn.otech.profile.repository;

import com.vn.otech.entity.UserFollow;
import com.vn.otech.entity.UserFollowId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserFollowRepository extends JpaRepository<UserFollow, UserFollowId> {
    boolean existsByIdFollowerIdAndIdFollowingId(UUID followerId, UUID followingId);

    long countByIdFollowingId(UUID followingId);

    long countByIdFollowerId(UUID followerId);
}
