package com.vn.otech.group.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record GroupPageResponse(
        List<GroupResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
    public static GroupPageResponse from(Page<GroupResponse> page) {
        return new GroupPageResponse(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
