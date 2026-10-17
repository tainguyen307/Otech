package com.vn.otech.chat.dto;

import java.util.List;

public record MessagePageResponse(List<MessageResponse> items, boolean hasMore, String nextCursor) {
}
