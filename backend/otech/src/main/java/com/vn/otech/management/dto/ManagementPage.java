package com.vn.otech.management.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record ManagementPage<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> ManagementPage<T> from(Page<T> page) {
        return new ManagementPage<>(
                page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
