package com.vn.otech.management.repository;

import com.vn.otech.entity.Group;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface GroupManagementRepository extends JpaRepository<Group, UUID>,
        JpaSpecificationExecutor<Group> {
    @Override
    @EntityGraph(attributePaths = "owner")
    org.springframework.data.domain.Page<Group> findAll(
            Specification<Group> specification, Pageable pageable);

    long countByHiddenTrue();
}
