package com.cc.brokerage.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RejectOrderIT extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void oversizedMarketBuyIsRejectedAndLeavesPortfolioUnchanged() throws Exception {
        mockMvc.perform(post(apiV1() + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"MARKET","size":1000000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("INSUFFICIENT_CASH"))
                .andExpect(jsonPath("$.amount").value("885800000.00"));

        assertReferencePortfolio();
    }

    @Test
    void oversizedMarketSellIsRejectedAndLeavesPortfolioUnchanged() throws Exception {
        mockMvc.perform(post(apiV1() + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"PAMP","side":"SELL","type":"MARKET","size":100}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("INSUFFICIENT_SHARES"))
                .andExpect(jsonPath("$.amount").value("92585.00"));

        mockMvc.perform(get(apiV1() + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("627500.00"))
                .andExpect(jsonPath("$.totalValue").value("889756.00"))
                .andExpect(jsonPath("$.positions[?(@.ticker == 'PAMP')].quantity").value(40));
    }

    @Test
    void amountThatYieldsZeroSharesIsRejected() throws Exception {
        mockMvc.perform(post(apiV1() + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"MARKET","amount":"100"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("ZERO_SHARES"))
                .andExpect(jsonPath("$.size").value(0));

        assertReferencePortfolio();
    }

    @Test
    void reservedCashIsNotReusedAndOrderIsNotDownsized() throws Exception {
        mockMvc.perform(post(apiV1() + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"MARKET","size":850}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("INSUFFICIENT_CASH"))
                .andExpect(jsonPath("$.amount").value("752930.00"))
                .andExpect(jsonPath("$.size").value(850));

        assertReferencePortfolio();
    }

    @Test
    void secondOrderThatExceedsRemainingAvailableIsRejected() throws Exception {
        mockMvc.perform(post(apiV1() + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":400,"price":"800.00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"));

        mockMvc.perform(post(apiV1() + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":400,"price":"800.00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("INSUFFICIENT_CASH"))
                .andExpect(jsonPath("$.amount").value("320000.00"));

        mockMvc.perform(get(apiV1() + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("307500.00"))
                .andExpect(jsonPath("$.totalValue").value("889756.00"));
    }

    @Test
    void marketOrderWithoutMarketPriceIsRejectedAndTheAccountKeepsWorking() throws Exception {
        mockMvc.perform(post(apiV1() + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"PGR","side":"BUY","type":"MARKET","size":5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("NO_MARKET_PRICE"))
                .andExpect(jsonPath("$.price").doesNotExist());

        assertReferencePortfolio();

        mockMvc.perform(get(apiV1() + "/accounts/10001/orders"))
                .andExpect(status().isOk());

        mockMvc.perform(post(apiV1() + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"MARKET","size":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"));
    }

    @Test
    void ordersWithASideOrStatusUnknownToTheServiceAreLeftOutOfThePortfolio() throws Exception {
        jdbc.update("INSERT INTO orders (instrumentId, userId, size, price, side, status, type, datetime) "
                + "VALUES (47, 1, 7, 900, 'DIVIDEND', 'FILLED', 'MARKET', '2023-07-14 10:00:00')");
        jdbc.update("INSERT INTO orders (instrumentId, userId, size, price, side, status, type, datetime) "
                + "VALUES (47, 1, 7, 900, 'BUY', 'PARTIAL', 'MARKET', '2023-07-14 10:01:00')");

        assertReferencePortfolio();
        mockMvc.perform(get(apiV1() + "/accounts/10001/orders").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[11].side").value("UNKNOWN"))
                .andExpect(jsonPath("$.content[12].status").value("UNKNOWN"));
    }

    @Test
    void instrumentTypeUnknownToTheServiceDoesNotBreakReads() throws Exception {
        jdbc.update("INSERT INTO instruments (ticker, name, type) VALUES ('ZZBOND', 'Bono de prueba', 'BONOS')");
        try {
            mockMvc.perform(get(apiV1() + "/instruments").param("q", "ZZBOND"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].ticker").value("ZZBOND"))
                    .andExpect(jsonPath("$[0].type").value("UNKNOWN"));
        } finally {
            jdbc.update("DELETE FROM instruments WHERE ticker = 'ZZBOND'");
        }
    }

    private void assertReferencePortfolio() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("627500.00"))
                .andExpect(jsonPath("$.totalValue").value("889756.00"));
    }
}
