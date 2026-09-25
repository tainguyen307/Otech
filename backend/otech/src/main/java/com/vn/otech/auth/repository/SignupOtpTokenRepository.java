package com.vn.otech.auth.repository;

import com.vn.otech.entity.SignupOtpToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignupOtpTokenRepository extends JpaRepository<SignupOtpToken, UUID> {
    Optional<SignupOtpToken> findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(UUID userId);
}