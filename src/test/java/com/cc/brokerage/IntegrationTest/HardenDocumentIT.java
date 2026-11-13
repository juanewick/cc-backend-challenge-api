package com.cc.brokerage.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HardenDocumentIT extends IntegrationTestBase {

    private String orders() {
        return apiV1() + "/accounts/10001/orders";
    }

    private static final String UNREADABLE = "The request cannot be read.";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unreadableBodyIsRejectedWithoutEchoingIt() throws Exception {
        reject(MediaType.APPLICATION_JSON, "not-a-document", UNREADABLE, "not-a-document");
    }

    @Test
    void emptyBodyIsRejectedAsUnreadable() throws Exception {
        reject(MediaType.APPLICATION_JSON, "", UNREADABLE, null);
    }

    @Test
    void nonObjectDocumentIsRejectedAsUnreadable() throws Exception {
        reject(MediaType.APPLICATION_JSON, "[1,2]", UNREADABLE, "[1,2]");
    }

    @Test
    void unsupportedContentTypeIsA415() throws Exception {
        Integer before = jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);

        MvcResult result = mockMvc.perform(post(orders())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(validOrder()))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().string("Accept", containsString("application/json")))
                .andExpect(jsonPath("$.type").value("/v3/api-docs#/components/schemas/UnsupportedMediaType"))
                .andExpect(jsonPath("$.title").value("Unsupported media type"))
                .andExpect(jsonPath("$.status").value(415))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.detail").value("The request body must be sent as application/json."))
                .andExpect(jsonPath("$.invalid_params").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("GGAL");
        assertThat(jdbcTemplate.queryForObject("select count(*) from orders", Integer.class)).isEqualTo(before);
    }

    @Test
    void trailingTokenIsRejectedAsUnreadable() throws Exception {
        reject(MediaType.APPLICATION_JSON, validOrder() + " true", UNREADABLE, "true");
    }

    @Test
    void unknownFieldIsRejectedWithoutNamingIt() throws Exception {
        String body = """
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"1.00","foreignField":1}
                """;
        reject(MediaType.APPLICATION_JSON, body, "The request contains a field that is not allowed.", "foreignField");
    }

    @Test
    void duplicateSizeIsRejected() throws Exception {
        String body = """
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"size":2,"price":"1.00"}
                """;
        reject(MediaType.APPLICATION_JSON, body, "The size field is duplicated.", "2");
    }

    @Test
    void duplicateUnknownFieldIsRejectedWithoutNamingIt() throws Exception {
        String body = """
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"1.00","foreignField":1,"foreignField":2}
                """;
        reject(MediaType.APPLICATION_JSON, body, "The request contains a duplicated field.", "foreignField");
    }

    @Test
    void sameNameInDifferentObjectsIsNotADuplicate() throws Exception {
        String body = """
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"1.00","extra":{"ticker":"X"}}
                """;
        reject(MediaType.APPLICATION_JSON, body, "The request contains a field that is not allowed.", "extra");
    }

    @Test
    void numericTickerIsRejected() throws Exception {
        String body = """
                {"ticker":12345,"side":"BUY","type":"LIMIT","size":1,"price":"1.00"}
                """;
        reject(MediaType.APPLICATION_JSON, body, "The ticker field is not valid.", "12345");
    }

    @Test
    void textualSizeIsRejected() throws Exception {
        String body = """
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":"not-a-size","price":"1.00"}
                """;
        reject(MediaType.APPLICATION_JSON, body, "The size field is not valid.", "not-a-size");
    }

    private static String validOrder() {
        return """
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"1.00"}""";
    }

    private void reject(MediaType contentType, String body, String message, String absent) throws Exception {
        Integer before = jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);

        MvcResult result = mockMvc.perform(post(orders())
                        .contentType(contentType)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(message))
                .andReturn();

        if (absent != null) {
            assertThat(result.getResponse().getContentAsString()).doesNotContain(absent);
        }
        Integer after = jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);
        assertThat(after).isEqualTo(before);
    }
}
