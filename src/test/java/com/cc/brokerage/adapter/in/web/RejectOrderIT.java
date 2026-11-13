package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RejectOrderIT extends IntegrationTestBase {

    @Test
    void oversizedMarketBuyIsRejectedAndLeavesPortfolioUnchanged() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
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
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"PAMP","side":"SELL","type":"MARKET","size":100}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("INSUFFICIENT_SHARES"))
                .andExpect(jsonPath("$.amount").value("92585.00"));

        mockMvc.perform(get(API_V1 + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("627500.00"))
                .andExpect(jsonPath("$.totalValue").value("889756.00"))
                .andExpect(jsonPath("$.positions[?(@.ticker == 'PAMP')].quantity").value(40));
    }

    @Test
    void amountThatYieldsZeroSharesIsRejected() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
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
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
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
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":400,"price":"800.00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"));

        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":400,"price":"800.00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("INSUFFICIENT_CASH"))
                .andExpect(jsonPath("$.amount").value("320000.00"));

        mockMvc.perform(get(API_V1 + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("307500.00"))
                .andExpect(jsonPath("$.totalValue").value("889756.00"));
    }

    private void assertReferencePortfolio() throws Exception {
        mockMvc.perform(get(API_V1 + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("627500.00"))
                .andExpect(jsonPath("$.totalValue").value("889756.00"));
    }
}
