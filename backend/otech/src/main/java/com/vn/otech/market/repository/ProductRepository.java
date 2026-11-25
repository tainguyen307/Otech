package com.vn.otech.market.repository;

import com.vn.otech.entity.Product;
import com.vn.otech.entity.ProductStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID>, ProductSearchRepository,
        JpaSpecificationExecutor<Product> {
    @EntityGraph(attributePaths = {"seller", "category"})
    List<Product> findBySellerIdOrderByUpdatedAtDesc(UUID sellerId);

    @EntityGraph(attributePaths = {"seller", "category"})
    Optional<Product> findWithSellerAndCategoryById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select product from Product product
            join fetch product.seller
            left join fetch product.category
            where product.id = :id
            """)
    Optional<Product> findWithSellerAndCategoryByIdForUpdate(@Param("id") UUID id);

    long countBySeller_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            UUID sellerId, LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"seller", "category"})
    List<Product> findByStatusAndBoostedUntilAfterOrderByBoostedUntilDescCreatedAtDesc(
            ProductStatus status, LocalDateTime now, Pageable pageable);

    @EntityGraph(attributePaths = {"seller", "category"})
    List<Product> findByStatusAndBoostedUntilAfterAndCategory_IdOrderByBoostedUntilDescCreatedAtDesc(
            ProductStatus status, LocalDateTime now, UUID categoryId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "seller")
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    long countByStatus(ProductStatus status);
}
