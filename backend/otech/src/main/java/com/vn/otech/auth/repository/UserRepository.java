package com.vn.otech.auth.repository;

import com.vn.otech.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = {"role", "role.rolePermissions", "role.rolePermissions.permission"})
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}