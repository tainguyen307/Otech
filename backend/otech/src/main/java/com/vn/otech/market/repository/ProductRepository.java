package com.vn.otech.market.repository;

import com.vn.otech.entity.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID>, ProductSearchRepository {
    @EntityGraph(attributePaths = {"seller", "category"})
    List<Product> findBySellerIdOrderByUpdatedAtDesc(UUID sellerId);

    @EntityGraph(attributePaths = {"seller", "category"})
    Optional<Product> findWithSellerAndCategoryById(UUID id);
}
