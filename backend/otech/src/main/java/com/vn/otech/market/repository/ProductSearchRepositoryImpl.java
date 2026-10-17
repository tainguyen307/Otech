package com.vn.otech.market.repository;

import com.vn.otech.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class ProductSearchRepositoryImpl implements ProductSearchRepository {
    private static final List<String> SORTS = List.of("newest", "oldest", "price-asc", "price-desc");

    private final EntityManager entityManager;

    public ProductSearchRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Product> search(ProductSearchFilter filter) {
        if (!SORTS.contains(filter.sort())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported product sort");
        }

        ProductCursor cursor = decodeCursor(filter.cursor(), filter.sort());
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Product> query = builder.createQuery(Product.class);
        Root<Product> product = query.from(Product.class);
        product.fetch("seller", JoinType.INNER);
        product.fetch("category", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        if (filter.status() != null) {
            predicates.add(builder.equal(product.get("status"), filter.status()));
        }
        if (filter.categoryId() != null) {
            predicates.add(builder.equal(product.get("category").get("id"), filter.categoryId()));
        }
        if (filter.sellerId() != null) {
            predicates.add(builder.equal(product.get("seller").get("id"), filter.sellerId()));
        }
        if (filter.minPrice() != null) {
            predicates.add(builder.greaterThanOrEqualTo(product.get("price"), filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            predicates.add(builder.lessThanOrEqualTo(product.get("price"), filter.maxPrice()));
        }
        if (filter.condition() != null) {
            predicates.add(builder.equal(product.get("condition"), filter.condition()));
        }
        if (hasText(filter.location())) {
            predicates.add(builder.like(builder.lower(product.get("location")),
                    containsPattern(filter.location()), '\\'));
        }
        if (hasText(filter.search())) {
            String pattern = containsPattern(filter.search());
            predicates.add(builder.or(
                    builder.like(builder.lower(product.get("title")), pattern, '\\'),
                    builder.like(builder.lower(product.get("description")), pattern, '\\')));
        }
        if (cursor != null) {
            predicates.add(cursorPredicate(builder, product, cursor, filter.sort()));
        }

        query.select(product).where(predicates.toArray(Predicate[]::new)).distinct(true);
        query.orderBy(orderBy(builder, product, filter.sort()));

        TypedQuery<Product> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(filter.limit() + 1);
        return typedQuery.getResultList();
    }

    private Predicate cursorPredicate(CriteriaBuilder builder, Root<Product> product,
                                      ProductCursor cursor, String sort) {
        Path<UUID> id = product.get("id");
        if (sort.startsWith("price")) {
            Path<BigDecimal> price = product.get("price");
            BigDecimal cursorPrice = new BigDecimal(cursor.value());
            boolean ascending = sort.equals("price-asc");
            Predicate beyondPrice = ascending
                    ? builder.greaterThan(price, cursorPrice)
                    : builder.lessThan(price, cursorPrice);
            Predicate samePrice = builder.and(
                    builder.equal(price, cursorPrice),
                    ascending ? builder.greaterThan(id, cursor.id()) : builder.lessThan(id, cursor.id()));
            return builder.or(beyondPrice, samePrice);
        }

        Path<LocalDateTime> createdAt = product.get("createdAt");
        LocalDateTime cursorTime = LocalDateTime.parse(cursor.value());
        boolean ascending = sort.equals("oldest");
        Predicate beyondTime = ascending
                ? builder.greaterThan(createdAt, cursorTime)
                : builder.lessThan(createdAt, cursorTime);
        Predicate sameTime = builder.and(
                builder.equal(createdAt, cursorTime),
                ascending ? builder.greaterThan(id, cursor.id()) : builder.lessThan(id, cursor.id()));
        return builder.or(beyondTime, sameTime);
    }

    private List<Order> orderBy(CriteriaBuilder builder, Root<Product> product, String sort) {
        boolean ascending = sort.equals("oldest") || sort.equals("price-asc");
        Path<?> key = sort.startsWith("price") ? product.get("price") : product.get("createdAt");
        Order keyOrder = ascending ? builder.asc(key) : builder.desc(key);
        Order idOrder = ascending ? builder.asc(product.get("id")) : builder.desc(product.get("id"));
        return List.of(keyOrder, idOrder);
    }

    private String containsPattern(String value) {
        return "%" + value.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
    }

    private ProductCursor decodeCursor(String encoded, String expectedSort) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] fields = decoded.split("\\n", -1);
            if (fields.length != 3 || !fields[0].equals(expectedSort)) {
                throw new IllegalArgumentException();
            }
            UUID id = UUID.fromString(fields[2]);
            if (expectedSort.startsWith("price")) {
                new BigDecimal(fields[1]);
            } else {
                LocalDateTime.parse(fields[1]);
            }
            return new ProductCursor(fields[1], id);
        } catch (IllegalArgumentException | java.time.DateTimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product cursor");
        }
    }

    public static String encodeCursor(Product product, String sort) {
        String key = sort.startsWith("price")
                ? product.getPrice().toPlainString()
                : product.getCreatedAt().toString();
        String value = sort + "\n" + key + "\n" + product.getId();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ProductCursor(String value, UUID id) {
    }
}
