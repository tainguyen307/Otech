package com.vn.otech.auth.repository;

import com.vn.otech.entity.User;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.RoleName;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    @EntityGraph(attributePaths = {"role", "role.rolePermissions", "role.rolePermissions.permission"})
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Override
    @EntityGraph(attributePaths = "role")
    Page<User> findAll(Specification<User> specification, Pageable pageable);

    long countByStatus(AccountStatus status);

    long countByRole_Name(RoleName name);

    long countByRole_NameAndStatus(RoleName name, AccountStatus status);
}