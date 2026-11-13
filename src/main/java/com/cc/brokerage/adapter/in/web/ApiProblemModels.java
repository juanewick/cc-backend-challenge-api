package com.cc.brokerage.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public final class ApiProblemModels {

    private ApiProblemModels() {
    }

    @Schema(name = "InvalidParam", description = "API parameter that failed validation. The submitted value is not included.")
    public record InvalidParam(
            @Schema(example = "ticker") String field,
            @Schema(example = "The ticker is not valid.") String message
    ) {
    }

    @Schema(name = "InvalidParameters", description = "API validation. Includes invalid_params.")
    public record InvalidParameters(
            @Schema(example = ProblemTypes.INVALID_PARAMETERS) String type,
            @Schema(example = "Invalid parameters") String title,
            @Schema(example = "400") int status,
            @Schema(example = "The ticker is not valid.") String detail,
            @Schema(example = "BRK-009") String code,
            @JsonProperty("invalid_params") List<InvalidParam> invalidParams
    ) {
    }

    @Schema(name = "BadRequest", description = "Rejected request without invalid API parameters. Does not include invalid_params.")
    public record BadRequest(
            @Schema(example = ProblemTypes.BAD_REQUEST) String type,
            @Schema(example = "Bad request") String title,
            @Schema(example = "400") int status,
            @Schema(example = "New cash deposits and withdrawals are not accepted.") String detail,
            @Schema(example = "BRK-007") String code
    ) {
    }

    @Schema(name = "NotFound", description = "Missing resource. Does not include invalid_params.")
    public record NotFound(
            @Schema(example = ProblemTypes.NOT_FOUND) String type,
            @Schema(example = "Not found") String title,
            @Schema(example = "404") int status,
            @Schema(example = "Account not found.") String detail,
            @Schema(example = "BRK-004") String code
    ) {
    }

    @Schema(name = "Conflict", description = "The operation conflicts with the current state. Does not include invalid_params.")
    public record Conflict(
            @Schema(example = ProblemTypes.CONFLICT) String type,
            @Schema(example = "Conflict") String title,
            @Schema(example = "409") int status,
            @Schema(example = "The order does not belong to this account.") String detail,
            @Schema(example = "BRK-001") String code
    ) {
    }
}
