package com.vn.otech.management.repository;

import com.vn.otech.entity.Page;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface PageManagementRepository extends JpaRepository<com.vn.otech.entity.Page, UUID>,
        JpaSpecificationExecutor<com.vn.otech.entity.Page> {
    @Override
    @EntityGraph(attributePaths = "owner")
    org.springframework.data.domain.Page<Page> findAll(
            Specification<com.vn.otech.entity.Page> specification, Pageable pageable);

    long countByHiddenTrue();
}
