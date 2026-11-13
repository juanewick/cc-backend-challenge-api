package com.cc.brokerage.adapter.in.web.api;

final class ProblemTypes {

    static final String DOCS = "/v3/api-docs#/components/schemas/";
    static final String INVALID_PARAMETERS = DOCS + "InvalidParameters";
    static final String BAD_REQUEST = DOCS + "BadRequest";
    static final String NOT_FOUND = DOCS + "NotFound";
    static final String CONFLICT = DOCS + "Conflict";
    static final String UNSUPPORTED_MEDIA_TYPE = DOCS + "UnsupportedMediaType";
    static final String BLANK = "about:blank";

    private ProblemTypes() {
    }
}
