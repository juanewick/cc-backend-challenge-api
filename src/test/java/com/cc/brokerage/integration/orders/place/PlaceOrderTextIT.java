package com.cc.brokerage.integration.orders.place;

import com.cc.brokerage.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaceOrderTextIT extends IntegrationTestBase {

    private String orders() {
        return apiV1() + "/accounts/10001/orders";
    }

    private static final String INVALID_ACCOUNT = "The account is not valid.";
    private static final String INVALID_TICKER = "The ticker is not valid.";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void emptyTickerIsRejectedWithoutSaving() throws Exception {
        rejectTicker("");
    }

    @Test
    void tickerWithSpacesIsRejectedWithoutSaving() throws Exception {
        rejectTicker(" PAMP");
        rejectTicker("PAMP ");
    }

    @Test
    void tickerWithSignsIsRejectedWithoutSaving() throws Exception {
        rejectTicker("PAMP'; DROP TABLE orders;--");
    }

    @Test
    void tickerAboveTenCharactersIsRejectedWithoutSaving() throws Exception {
        rejectTicker("ABCDEFGHIJK");
    }

    @Test
    void unknownTickerDoesNotEchoTheSymbol() throws Exception {
        Integer before = orderCount();

        MvcResult result = mockMvc.perform(post(orders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order("ZZZZ")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRK-002"))
                .andExpect(jsonPath("$.detail").value("Instrument not found."))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("ZZZZ");
        assertThat(orderCount()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABCDE", "10001 ", " 10001", "10001' OR '1'='1", "123456789012345678901"})
    void invalidAccountIsRejectedWithoutSaving(String account) throws Exception {
        Integer before = orderCount();

        mockMvc.perform(post(apiV1() + "/accounts/{accountNumber}/orders", account)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order("GGAL")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-008"))
                .andExpect(jsonPath("$.detail").value(INVALID_ACCOUNT));

        assertThat(orderCount()).isEqualTo(before);
    }

    private void rejectTicker(String ticker) throws Exception {
        Integer before = orderCount();

        MvcResult result = mockMvc.perform(post(orders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order(ticker)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/v3/api-docs#/components/schemas/InvalidParameters"))
                .andExpect(jsonPath("$.title").value("Invalid parameters"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BRK-009"))
                .andExpect(jsonPath("$.detail").value(INVALID_TICKER))
                .andExpect(jsonPath("$.invalid_params[0].field").value("ticker"))
                .andExpect(jsonPath("$.invalid_params[0].message").value(INVALID_TICKER))
                .andReturn();

        if (!ticker.isEmpty()) {
            assertThat(result.getResponse().getContentAsString()).doesNotContain(ticker);
        }
        assertThat(orderCount()).isEqualTo(before);
    }

    private static String order(String ticker) {
        return """
                {"ticker":"%s","side":"BUY","type":"LIMIT","size":1,"price":"1.00"}
                """.formatted(ticker);
    }

    private Integer orderCount() {
        return jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);
    }
}
