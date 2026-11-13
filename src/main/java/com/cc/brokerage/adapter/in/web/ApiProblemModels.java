package com.cc.brokerage.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public final class ApiProblemModels {

    private ApiProblemModels() {
    }

    @Schema(name = "InvalidParam", description = "Parámetro de la API que no cumple la validación. No incluye el valor enviado.")
    public record InvalidParam(
            @Schema(example = "ticker") String field,
            @Schema(example = "El ticker no es válido") String message
    ) {
    }

    @Schema(name = "InvalidParameters", description = "Validación de la API. Incluye invalid_params.")
    public record InvalidParameters(
            @Schema(example = ProblemTypes.INVALID_PARAMETERS) String type,
            @Schema(example = "Parámetros inválidos") String title,
            @Schema(example = "400") int status,
            @Schema(example = "El ticker no es válido") String detail,
            @Schema(example = "INVALID_TICKER") String code,
            @JsonProperty("invalid_params") List<InvalidParam> invalidParams
    ) {
    }

    @Schema(name = "BadRequest", description = "Pedido rechazado sin parámetros de API inválidos. No incluye invalid_params.")
    public record BadRequest(
            @Schema(example = ProblemTypes.BAD_REQUEST) String type,
            @Schema(example = "Pedido inválido") String title,
            @Schema(example = "400") int status,
            @Schema(example = "No se registran ingresos ni egresos de pesos nuevos") String detail,
            @Schema(example = "CASH_MOVEMENT") String code
    ) {
    }

    @Schema(name = "NotFound", description = "Recurso inexistente. No incluye invalid_params.")
    public record NotFound(
            @Schema(example = ProblemTypes.NOT_FOUND) String type,
            @Schema(example = "No encontrado") String title,
            @Schema(example = "404") int status,
            @Schema(example = "La cuenta no existe") String detail,
            @Schema(example = "ACCOUNT_NOT_FOUND") String code
    ) {
    }

    @Schema(name = "Conflict", description = "La operación choca con el estado actual. No incluye invalid_params.")
    public record Conflict(
            @Schema(example = ProblemTypes.CONFLICT) String type,
            @Schema(example = "Conflicto") String title,
            @Schema(example = "409") int status,
            @Schema(example = "La orden no pertenece a esa cuenta") String detail,
            @Schema(example = "ORDER_NOT_OWNED") String code
    ) {
    }
}
