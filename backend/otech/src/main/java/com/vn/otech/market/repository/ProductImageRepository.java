package com.vn.otech.market.repository;

import com.vn.otech.entity.ProductImage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {
    List<ProductImage> findByProductIdInOrderByDisplayOrderAsc(Collection<UUID> productIds);

    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(UUID productId);

    void deleteByProductId(UUID productId);
}
