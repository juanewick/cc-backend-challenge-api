package com.cc.brokerage.adapter.in.web.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "One page of orders, oldest first.")
public record OrderPageResponse(
        List<OrderResponse> content,
        @Schema(description = "Page number, from 0.") int page,
        @Schema(description = "Orders per page.") int pageSize,
        @Schema(description = "Orders matching the filter, across all pages.") long totalElements,
        int totalPages
) {
}
