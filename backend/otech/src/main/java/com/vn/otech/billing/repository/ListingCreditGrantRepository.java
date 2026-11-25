package com.vn.otech.billing.repository;

import com.vn.otech.billing.entity.ListingCreditGrant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ListingCreditGrantRepository extends JpaRepository<ListingCreditGrant, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select creditGrant from ListingCreditGrant creditGrant
            where creditGrant.user.id = :userId
              and creditGrant.creditsRemaining > 0
              and creditGrant.validUntil > :now
            order by creditGrant.validUntil asc, creditGrant.createdAt asc
            """)
    List<ListingCreditGrant> findUsableForUpdate(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now);

    @Query("""
            select coalesce(sum(creditGrant.creditsRemaining), 0)
            from ListingCreditGrant creditGrant
            where creditGrant.user.id = :userId
              and creditGrant.creditsRemaining > 0
              and creditGrant.validUntil > :now
            """)
    long countUsableCredits(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
