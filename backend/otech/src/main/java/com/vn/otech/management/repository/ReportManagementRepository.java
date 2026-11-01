package com.vn.otech.management.repository;

import com.vn.otech.entity.Report;
import com.vn.otech.entity.ReportStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface ReportManagementRepository extends JpaRepository<Report, UUID>,
        JpaSpecificationExecutor<Report> {
    @Override
    @EntityGraph(attributePaths = "reporter")
    Page<Report> findAll(Specification<Report> specification, Pageable pageable);

    long countByStatus(ReportStatus status);
}
