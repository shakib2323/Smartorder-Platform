package com.shakib.smartorder.product.dto;

import java.util.List;

import org.springframework.data.domain.Page;

// Every list endpoint returns this, so clients always know the total and can build page controls. 
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages());
    }
}
