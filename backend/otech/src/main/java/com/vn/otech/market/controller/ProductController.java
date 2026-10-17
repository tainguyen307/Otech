package com.vn.otech.market.controller;

import com.vn.otech.entity.ProductCondition;
import com.vn.otech.entity.ProductStatus;
import com.vn.otech.market.dto.CategoryResponse;
import com.vn.otech.market.dto.ProductImageUploadResponse;
import com.vn.otech.market.dto.ProductPageResponse;
import com.vn.otech.market.dto.ProductRequest;
import com.vn.otech.market.dto.ProductResponse;
import com.vn.otech.market.dto.ProductStatusRequest;
import com.vn.otech.market.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/api")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return productService.listCategories();
    }

    @GetMapping("/products")
    public ProductPageResponse search(
            @RequestParam(required = false) @Size(max = 120) String search,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) ProductCondition condition,
            @RequestParam(required = false) @Size(max = 120) String location,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "newest") @Size(max = 20) String sort,
            @RequestParam(required = false) @Size(max = 256) String cursor,
            @RequestParam(defaultValue = "24") @Min(1) @Max(48) int limit) {
        return productService.search(search, categoryId, minPrice, maxPrice, condition, location,
                status, sort, cursor, limit);
    }

    @GetMapping("/products/{productId}")
    public ProductResponse product(@PathVariable UUID productId) {
        return productService.getPublicProduct(productId);
    }

    @GetMapping("/user/products")
    public ProductPageResponse myProducts(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) @Size(max = 256) String cursor,
            @RequestParam(defaultValue = "24") @Min(1) @Max(48) int limit) {
        return productService.listMyProducts(principal.getUsername(), cursor, limit);
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.create(principal.getUsername(), request);
        return ResponseEntity.created(URI.create("/api/products/" + product.id())).body(product);
    }

    @PutMapping("/products/{productId}")
    public ProductResponse update(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest request) {
        return productService.update(principal.getUsername(), productId, request, principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).toList());
    }

    @PatchMapping("/products/{productId}/status")
    public ProductResponse updateStatus(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductStatusRequest request) {
        return productService.updateStatus(principal.getUsername(), productId, request, principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).toList());
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID productId) {
        productService.hide(principal.getUsername(), productId, principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).toList());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/products/images")
    public ResponseEntity<ProductImageUploadResponse> uploadImage(
            @AuthenticationPrincipal UserDetails principal,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.uploadImage(principal.getUsername(), file));
    }
}
