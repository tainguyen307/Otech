package com.vn.otech.market.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductReviewsResponse(
        double averageRating,
        long reviewCount,
        List<Review> reviews) {
    public record Review(
            UUID id,
            String reviewerName,
            int rating,
            String comment,
            LocalDateTime createdAt) {
    }
}
