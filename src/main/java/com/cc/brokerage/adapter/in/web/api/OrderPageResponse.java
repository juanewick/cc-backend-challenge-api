package com.cc.brokerage.adapter.in.web.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Una página de órdenes, de la más antigua a la más nueva.")
public record OrderPageResponse(
        List<OrderResponse> content,
        @Schema(description = "Número de página, desde 0.") int page,
        @Schema(description = "Órdenes por página.") int pageSize,
        @Schema(description = "Órdenes que cumplen el filtro, en todas las páginas.") long totalElements,
        int totalPages
) {
}
