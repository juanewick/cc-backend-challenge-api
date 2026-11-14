package com.cc.brokerage.adapter.in.web.api;

import com.cc.brokerage.domain.model.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

@Schema(description = "One page of results.")
public record PageResponse<T>(
        List<T> content,
        @Schema(description = "Page number, from 0.") int page,
        @Schema(description = "Items per page.") int pageSize,
        @Schema(description = "Items matching the request, across all pages.") long totalElements,
        int totalPages
) {

    public static <T, R> PageResponse<R> from(PageResult<T> found, Function<? super T, ? extends R> mapper) {
        return new PageResponse<>(
                found.content().stream().<R>map(mapper).toList(),
                found.page(),
                found.size(),
                found.totalElements(),
                found.totalPages()
        );
    }
}
