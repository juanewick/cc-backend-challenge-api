package com.cc.brokerage.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HardenDocumentIT extends IntegrationTestBase {

    private static final String ORDERS = API_V1 + "/accounts/10001/orders";
    private static final String UNREADABLE = "El pedido no se puede leer";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void unreadableBodyIsRejectedWithoutEchoingIt() throws Exception {
        reject(MediaType.APPLICATION_JSON, "no-es-un-documento", UNREADABLE, "no-es-un-documento");
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
    void unsupportedContentTypeIsRejectedAsUnreadable() throws Exception {
        reject(MediaType.TEXT_PLAIN, validOrder(), UNREADABLE, "GGAL");
    }

    @Test
    void trailingTokenIsRejectedAsUnreadable() throws Exception {
        reject(MediaType.APPLICATION_JSON, validOrder() + " true", UNREADABLE, "true");
    }

    @Test
    void unknownFieldIsRejectedWithoutNamingIt() throws Exception {
        String body = """
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"1.00","campoAjeno":1}
                """;
        reject(MediaType.APPLICATION_JSON, body, "El pedido tiene un campo que no corresponde", "campoAjeno");
    }

    @Test
    void duplicateSizeIsRejected() throws Exception {
        String body = """
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"size":2,"price":"1.00"}
                """;
        reject(MediaType.APPLICATION_JSON, body, "El campo cantidad está repetido", "2");
    }

    @Test
    void numericTickerIsRejected() throws Exception {
        String body = """
                {"ticker":12345,"side":"BUY","type":"LIMIT","size":1,"price":"1.00"}
                """;
        reject(MediaType.APPLICATION_JSON, body, "El campo ticker no es válido", "12345");
    }

    @Test
    void textualSizeIsRejected() throws Exception {
        String body = """
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":"no-es-cantidad","price":"1.00"}
                """;
        reject(MediaType.APPLICATION_JSON, body, "El campo cantidad no es válido", "no-es-cantidad");
    }

    private static String validOrder() {
        return """
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"1.00"}""";
    }

    private void reject(MediaType contentType, String body, String message, String absent) throws Exception {
        Integer before = jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);

        MvcResult result = mockMvc.perform(post(ORDERS)
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
