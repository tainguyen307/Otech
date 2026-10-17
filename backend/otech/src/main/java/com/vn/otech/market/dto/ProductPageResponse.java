package com.vn.otech.market.dto;

import java.util.List;

public record ProductPageResponse(List<ProductResponse> items, boolean hasMore, String nextCursor) {
}
