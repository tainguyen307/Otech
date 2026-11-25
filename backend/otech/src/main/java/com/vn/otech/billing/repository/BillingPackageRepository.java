package com.vn.otech.billing.repository;

import com.vn.otech.billing.entity.BillingPackage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingPackageRepository extends JpaRepository<BillingPackage, UUID> {
    List<BillingPackage> findByActiveTrueOrderByPriceAsc();

    List<BillingPackage> findAllByOrderByCreatedAtDesc();

    Optional<BillingPackage> findByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select billingPackage from BillingPackage billingPackage where billingPackage.id = :id")
    Optional<BillingPackage> findByIdForUpdate(@Param("id") UUID id);
}
