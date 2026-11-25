package com.vn.otech.billing.repository;

import com.vn.otech.billing.entity.BillingPaymentRequest;
import com.vn.otech.billing.entity.PaymentRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingPaymentRequestRepository extends JpaRepository<BillingPaymentRequest, UUID> {
    @EntityGraph(attributePaths = {"user", "billingPackage", "product"})
    List<BillingPaymentRequest> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    @EntityGraph(attributePaths = {"user", "billingPackage", "product"})
    List<BillingPaymentRequest> findByStatusOrderByCreatedAtAsc(PaymentRequestStatus status);

    @EntityGraph(attributePaths = {"user", "billingPackage", "product"})
    Optional<BillingPaymentRequest> findWithDetailsById(UUID id);

    long countByBillingPackage_IdAndStatus(UUID packageId, PaymentRequestStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from BillingPaymentRequest request where request.id = :id")
    Optional<BillingPaymentRequest> findByIdForUpdate(@Param("id") UUID id);
}
