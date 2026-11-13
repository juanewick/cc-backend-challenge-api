package com.cc.brokerage.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HardenTextIT extends IntegrationTestBase {

    private static final String ORDERS = API_V1 + "/accounts/10001/orders";
    private static final String INVALID_ACCOUNT = "The account is not valid.";
    private static final String INVALID_TICKER = "The ticker is not valid.";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void accountWithLettersIsRejectedEverywhere() throws Exception {
        rejectAccount("ABCDE");
    }

    @Test
    void accountWithSpacesIsRejectedEverywhere() throws Exception {
        rejectAccount("10001 ");
        rejectAccount(" 10001");
    }

    @Test
    void accountWithSignsIsRejectedEverywhere() throws Exception {
        rejectAccount("10001' OR '1'='1");
    }

    @Test
    void accountAboveTwentyDigitsIsRejectedEverywhere() throws Exception {
        rejectAccount("123456789012345678901");
    }

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
    void unknownAccountDoesNotEchoTheNumber() throws Exception {
        MvcResult result = mockMvc.perform(get(API_V1 + "/accounts/{accountNumber}/portfolio", "99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/v3/api-docs#/components/schemas/NotFound"))
                .andExpect(jsonPath("$.title").value("Not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("BRK-004"))
                .andExpect(jsonPath("$.detail").value("Account not found."))
                .andExpect(jsonPath("$.invalid_params").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("99999");
    }

    @Test
    void unknownTickerDoesNotEchoTheSymbol() throws Exception {
        Integer before = orderCount();

        MvcResult result = mockMvc.perform(post(ORDERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order("ZZZZ")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRK-002"))
                .andExpect(jsonPath("$.detail").value("Instrument not found."))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("ZZZZ");
        assertThat(orderCount()).isEqualTo(before);
    }

    @Test
    void referenceAccountStillReturnsThePortfolio() throws Exception {
        mockMvc.perform(get(API_V1 + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("10001"))
                .andExpect(jsonPath("$.availableCash").value("627500.00"));
    }

    @Test
    void referenceSearchStillReturnsPamp() throws Exception {
        mockMvc.perform(get(API_V1 + "/instruments").param("q", "PAMP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("PAMP"))
                .andExpect(jsonPath("$[0].lastPrice").value("925.85"));
    }

    private void rejectAccount(String account) throws Exception {
        Integer before = orderCount();

        MvcResult portfolio = mockMvc.perform(get(API_V1 + "/accounts/{accountNumber}/portfolio", account))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-008"))
                .andExpect(jsonPath("$.detail").value(INVALID_ACCOUNT))
                .andReturn();
        assertThat(portfolio.getResponse().getContentAsString()).doesNotContain(account);
        assertThat(portfolio.getResponse().getContentAsString()).doesNotContain("627500.00");

        mockMvc.perform(post(API_V1 + "/accounts/{accountNumber}/orders", account)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(order("GGAL")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-008"))
                .andExpect(jsonPath("$.detail").value(INVALID_ACCOUNT));

        mockMvc.perform(post(API_V1 + "/accounts/{accountNumber}/orders/5/cancellation", account))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-008"))
                .andExpect(jsonPath("$.detail").value(INVALID_ACCOUNT));

        assertThat(orderCount()).isEqualTo(before);
        assertThat(statusOf(5L)).isEqualTo("NEW");
    }

    private void rejectTicker(String ticker) throws Exception {
        Integer before = orderCount();

        MvcResult result = mockMvc.perform(post(ORDERS)
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

    private String statusOf(long orderId) {
        return jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, orderId);
    }
}
