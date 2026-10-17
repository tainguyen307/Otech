package com.vn.otech.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.vn.otech.auth.repository.UserRepository;
import com.vn.otech.entity.Product;
import com.vn.otech.entity.ProductCondition;
import com.vn.otech.entity.ProductStatus;
import com.vn.otech.entity.User;
import com.vn.otech.market.dto.ProductPageResponse;
import com.vn.otech.market.dto.ProductStatusRequest;
import com.vn.otech.market.repository.CategoryRepository;
import com.vn.otech.market.repository.ProductImageRepository;
import com.vn.otech.market.repository.ProductRepository;
import com.vn.otech.market.repository.ProductSearchFilter;
import com.vn.otech.market.service.ProductService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;
    @Mock private ObjectProvider<Cloudinary> cloudinaryProvider;
    @InjectMocks private ProductService productService;

    @Test
    void searchReturnsCursorAndDefaultsToSellingListings() {
        Product first = product("Desk", ProductStatus.SELLING);
        Product second = product("Lamp", ProductStatus.SELLING);
        when(productRepository.search(any(ProductSearchFilter.class))).thenReturn(List.of(first, second));
        when(productImageRepository.findByProductIdInOrderByDisplayOrderAsc(any())).thenReturn(List.of());

        ProductPageResponse response = productService.search(
                "chair", null, null, null, null, null, null, null, null, 1);

        assertEquals(1, response.items().size());
        assertEquals("Desk", response.items().getFirst().title());
        assertTrue(response.hasMore());
        assertNotNull(response.nextCursor());
        verify(productRepository).search(any(ProductSearchFilter.class));
    }

    @Test
    void searchRejectsHiddenStatusAndInvertedPriceRange() {
        assertThrows(ResponseStatusException.class, () -> productService.search(
                null, null, null, null, null, null, ProductStatus.HIDDEN, "newest", null, 24));
        assertThrows(ResponseStatusException.class, () -> productService.search(
                null, null, BigDecimal.TEN, BigDecimal.ONE, null, null, null, "newest", null, 24));
    }

    @Test
    void sellersCanChangeTheirListingStatus() {
        UUID productId = UUID.randomUUID();
        Product product = product("Chair", ProductStatus.SELLING);
        product.setId(productId);
        product.getSeller().setEmail("seller@example.com");
        when(userRepository.findByEmailIgnoreCase("seller@example.com"))
                .thenReturn(Optional.of(product.getSeller()));
        when(productRepository.findWithSellerAndCategoryById(productId)).thenReturn(Optional.of(product));
        when(productRepository.saveAndFlush(product)).thenReturn(product);
        when(productImageRepository.findByProductIdInOrderByDisplayOrderAsc(any())).thenReturn(List.of());

        var response = productService.updateStatus(
                "seller@example.com", productId, new ProductStatusRequest(ProductStatus.SOLD), List.of("ROLE_USER"));

        assertEquals(ProductStatus.SOLD, response.status());
        assertTrue(response.imageUrls().isEmpty());
    }

    @Test
    void otherUsersCannotChangeListingStatus() {
        UUID productId = UUID.randomUUID();
        Product product = product("Chair", ProductStatus.SELLING);
        product.setId(productId);
        product.getSeller().setEmail("seller@example.com");
        User requester = new User();
        requester.setId(UUID.randomUUID());
        requester.setEmail("other@example.com");
        requester.setStatus(com.vn.otech.entity.AccountStatus.ACTIVE);
        when(userRepository.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(requester));
        when(productRepository.findWithSellerAndCategoryById(productId)).thenReturn(Optional.of(product));

        assertThrows(ResponseStatusException.class, () -> productService.updateStatus(
                "other@example.com", productId, new ProductStatusRequest(ProductStatus.SOLD), List.of("ROLE_USER")));
    }

    private Product product(String title, ProductStatus status) {
        User seller = new User();
        seller.setId(UUID.randomUUID());
        seller.setFullName("Seller");
        seller.setEmail("seller@example.com");
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setSeller(seller);
        product.setTitle(title);
        product.setPrice(BigDecimal.valueOf(250_000));
        product.setCondition(ProductCondition.GOOD);
        product.setStatus(status);
        product.setCreatedAt(LocalDateTime.of(2026, 10, 1, 12, 0));
        product.setUpdatedAt(product.getCreatedAt());
        return product;
    }
}
