package com.vn.otech.group.repository;

import com.vn.otech.entity.GroupPost;
import com.vn.otech.entity.GroupPostStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupPostRepository extends JpaRepository<GroupPost, UUID> {
    @EntityGraph(attributePaths = {"author"})
    List<GroupPost> findByGroup_IdAndStatusOrderByCreatedAtDesc(UUID groupId, GroupPostStatus status);

    @EntityGraph(attributePaths = {"author"})
    List<GroupPost> findByGroup_IdOrderByCreatedAtDesc(UUID groupId);

    Optional<GroupPost> findByIdAndGroup_Id(UUID postId, UUID groupId);

    long countByGroup_IdAndStatus(UUID groupId, GroupPostStatus status);

    void deleteByGroup_Id(UUID groupId);
}
