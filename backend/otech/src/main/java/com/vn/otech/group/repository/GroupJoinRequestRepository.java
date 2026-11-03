package com.vn.otech.group.repository;

import com.vn.otech.entity.GroupJoinRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, UUID> {
    boolean existsByGroup_IdAndUser_IdAndStatus(UUID groupId, UUID userId, String status);

    Optional<GroupJoinRequest> findByGroup_IdAndUser_IdAndStatus(UUID groupId, UUID userId, String status);

    Optional<GroupJoinRequest> findByIdAndGroup_Id(UUID requestId, UUID groupId);

    @EntityGraph(attributePaths = {"user"})
    List<GroupJoinRequest> findByGroup_IdAndStatusOrderById(UUID groupId, String status);

    void deleteByGroup_Id(UUID groupId);
}
