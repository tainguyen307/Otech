package com.vn.otech.group.repository;

import com.vn.otech.entity.GroupMember;
import com.vn.otech.entity.GroupMemberId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMember, GroupMemberId> {
    Optional<GroupMember> findById_GroupIdAndId_UserId(UUID groupId, UUID userId);

    boolean existsById_GroupIdAndId_UserId(UUID groupId, UUID userId);

    long countById_GroupId(UUID groupId);

    @EntityGraph(attributePaths = {"user"})
    List<GroupMember> findById_GroupIdOrderByUser_FullName(UUID groupId);
}
