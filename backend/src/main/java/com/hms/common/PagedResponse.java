package com.hms.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(List<T> content, long totalElements, int totalPages, int page, int size) {

    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}