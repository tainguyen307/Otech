package com.vn.otech.market.service;

import com.cloudinary.Cloudinary;
import com.vn.otech.entity.Category;
import com.vn.otech.entity.AccountStatus;
import com.vn.otech.entity.Product;
import com.vn.otech.entity.ProductCondition;
import com.vn.otech.entity.ProductImage;
import com.vn.otech.entity.ProductStatus;
import com.vn.otech.entity.User;
import com.vn.otech.market.dto.CategoryResponse;
import com.vn.otech.market.dto.ProductImageUploadResponse;
import com.vn.otech.market.dto.ProductPageResponse;
import com.vn.otech.market.dto.ProductRequest;
import com.vn.otech.market.dto.ProductResponse;
import com.vn.otech.market.dto.ProductStatusRequest;
import com.vn.otech.market.repository.CategoryRepository;
import com.vn.otech.market.repository.ProductImageRepository;
import com.vn.otech.market.repository.ProductRepository;
import com.vn.otech.market.repository.ProductSearchFilter;
import com.vn.otech.market.repository.ProductSearchRepositoryImpl;
import com.vn.otech.auth.repository.UserRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private static final int MAX_PAGE_SIZE = 48;
    private static final long MAX_IMAGE_SIZE = 8L * 1024 * 1024;
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ObjectProvider<Cloudinary> cloudinaryProvider;

    public ProductService(ProductRepository productRepository,
                          ProductImageRepository productImageRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository,
                          ObjectProvider<Cloudinary> cloudinaryProvider) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.cloudinaryProvider = cloudinaryProvider;
    }

    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName(), category.getDescription()))
                .toList();
    }

    public ProductPageResponse search(String search, UUID categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                      ProductCondition condition, String location, ProductStatus status,
                                      String sort, String cursor, int limit) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum price cannot exceed maximum price");
        }
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prices cannot be negative");
        }
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        ProductStatus visibleStatus = status == null ? ProductStatus.SELLING : status;
        if (visibleStatus == ProductStatus.HIDDEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hidden listings are not publicly searchable");
        }
        ProductSearchFilter filter = new ProductSearchFilter(
                clean(search), categoryId, null, minPrice, maxPrice, condition, clean(location),
                visibleStatus, sort == null ? "newest" : sort, cursor, limit);
        return search(filter);
    }

    public ProductPageResponse listMyProducts(String email, String cursor, int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        User seller = findActiveUser(email);
        return search(new ProductSearchFilter(
                null, null, seller.getId(), null, null, null, null, null,
                "newest", cursor, limit));
    }

    private ProductPageResponse search(ProductSearchFilter filter) {
        int limit = filter.limit();
        List<Product> results = productRepository.search(filter);
        boolean hasMore = results.size() > limit;
        List<Product> page = hasMore ? results.subList(0, limit) : results;
        Map<UUID, List<String>> images = loadImages(page);
        List<ProductResponse> items = page.stream().map(product -> toResponse(product, images)).toList();
        String nextCursor = hasMore && !page.isEmpty()
                ? ProductSearchRepositoryImpl.encodeCursor(page.getLast(), filter.sort())
                : null;
        return new ProductPageResponse(items, hasMore, nextCursor);
    }

    public ProductResponse getPublicProduct(UUID productId) {
        Product product = findProduct(productId);
        if (product.getStatus() == ProductStatus.HIDDEN) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        return toResponse(product, loadImages(List.of(product)));
    }

    @Transactional
    public ProductResponse create(String email, ProductRequest request) {
        User seller = findActiveUser(email);
        Product product = new Product();
        product.setSeller(seller);
        applyRequest(product, request);
        product.setStatus(ProductStatus.SELLING);
        product = productRepository.saveAndFlush(product);
        replaceImages(product, request.imageUrls());
        return toResponse(product, loadImages(List.of(product)));
    }

    @Transactional
    public ProductResponse update(String email, UUID productId, ProductRequest request,
                                  Collection<String> authorities) {
        Product product = findProduct(productId);
        requireOwnerOrManager(product, email, authorities);
        applyRequest(product, request);
        product = productRepository.saveAndFlush(product);
        replaceImages(product, request.imageUrls());
        return toResponse(product, loadImages(List.of(product)));
    }

    @Transactional
    public ProductResponse updateStatus(String email, UUID productId, ProductStatusRequest request,
                                        Collection<String> authorities) {
        Product product = findProduct(productId);
        requireOwnerOrManager(product, email, authorities);
        product.setStatus(request.status());
        product = productRepository.saveAndFlush(product);
        return toResponse(product, loadImages(List.of(product)));
    }

    @Transactional
    public void hide(String email, UUID productId, Collection<String> authorities) {
        Product product = findProduct(productId);
        requireOwnerOrManager(product, email, authorities);
        product.setStatus(ProductStatus.HIDDEN);
        productRepository.save(product);
    }

    public ProductImageUploadResponse uploadImage(String email, MultipartFile file) {
        findActiveUser(email);
        if (file == null || file.isEmpty() || file.getContentType() == null
                || !IMAGE_CONTENT_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Upload a JPEG, PNG, or WebP image");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image must be 8 MB or smaller");
        }
        Cloudinary cloudinary = cloudinaryProvider.getIfAvailable();
        if (cloudinary == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Product image upload is not configured");
        }
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    Map.of("folder", "otech/products", "resource_type", "image"));
            Object imageUrl = result.get("secure_url");
            if (!(imageUrl instanceof String url) || url.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Image storage returned no image URL");
            }
            return new ProductImageUploadResponse(url);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not upload product image", exception);
        }
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setTitle(request.title().trim());
        product.setDescription(blankToNull(request.description()));
        product.setPrice(request.price());
        product.setCondition(request.condition());
        product.setLocation(blankToNull(request.location()));
        product.setCategory(request.categoryId() == null ? null : categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found")));
    }

    private void replaceImages(Product product, List<String> imageUrls) {
        productImageRepository.deleteByProductId(product.getId());
        if (imageUrls == null) {
            return;
        }
        List<ProductImage> images = new java.util.ArrayList<>();
        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.isBlank()) {
                continue;
            }
            String normalizedUrl = imageUrl.trim();
            if (images.stream().anyMatch(image -> image.getImageUrl().equals(normalizedUrl))) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(normalizedUrl);
            image.setDisplayOrder(images.size());
            images.add(image);
        }
        productImageRepository.saveAll(images);
    }

    private Map<UUID, List<String>> loadImages(Collection<Product> products) {
        if (products.isEmpty()) {
            return Map.of();
        }
        List<UUID> productIds = products.stream().map(Product::getId).toList();
        return productImageRepository.findByProductIdInOrderByDisplayOrderAsc(productIds).stream()
                .collect(Collectors.groupingBy(
                        image -> image.getProduct().getId(),
                        Collectors.mapping(ProductImage::getImageUrl, Collectors.toList())));
    }

    private ProductResponse toResponse(Product product, Map<UUID, List<String>> images) {
        Category category = product.getCategory();
        User seller = product.getSeller();
        return new ProductResponse(
                product.getId(), product.getTitle(), product.getDescription(), product.getPrice(),
                category == null ? null : category.getId(),
                category == null ? null : category.getName(),
                product.getCondition(), product.getLocation(), product.getStatus(),
                product.getCreatedAt(), product.getUpdatedAt(),
                images.getOrDefault(product.getId(), List.of()),
                new ProductResponse.SellerResponse(seller.getId(), seller.getFullName(), seller.getAvatarUrl()));
    }

    private Product findProduct(UUID productId) {
        return productRepository.findWithSellerAndCategoryById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private User findUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void requireOwnerOrManager(Product product, String email, Collection<String> authorities) {
        User requester = findActiveUser(email);
        boolean manager = authorities.contains("ROLE_MANAGER") || authorities.contains("ROLE_ADMIN");
        if (!manager && !product.getSeller().getId().equals(requester.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot manage this listing");
        }
    }

    private User findActiveUser(String email) {
        User user = findUser(email);
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is not active");
        }
        return user;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToNull(String value) {
        return clean(value);
    }
}
