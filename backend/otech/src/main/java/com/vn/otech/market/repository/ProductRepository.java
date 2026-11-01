package com.vn.otech.market.repository;

import com.vn.otech.entity.Product;
import com.vn.otech.entity.ProductStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

public interface ProductRepository extends JpaRepository<Product, UUID>, ProductSearchRepository,
        JpaSpecificationExecutor<Product> {
    @EntityGraph(attributePaths = {"seller", "category"})
    List<Product> findBySellerIdOrderByUpdatedAtDesc(UUID sellerId);

    @EntityGraph(attributePaths = {"seller", "category"})
    Optional<Product> findWithSellerAndCategoryById(UUID id);

    @Override
    @EntityGraph(attributePaths = "seller")
    Page<Product> findAll(Specification<Product> specification, Pageable pageable);

    long countByStatus(ProductStatus status);
}
