package com.vn.otech.market.repository;

import com.vn.otech.market.entity.ProductReview;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {
    @Query("""
            select coalesce(avg(review.rating), 0)
            from ProductReview review
            where review.product.id = :productId
            """)
    double averageRating(@Param("productId") UUID productId);

    long countByProduct_Id(UUID productId);

    @EntityGraph(attributePaths = {"reviewer", "product"})
    List<ProductReview> findByProduct_IdOrderByCreatedAtDesc(UUID productId);

    Optional<ProductReview> findByTransaction_Id(UUID transactionId);
}
