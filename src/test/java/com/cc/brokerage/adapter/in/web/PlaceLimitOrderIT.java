package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaceLimitOrderIT extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void limitBuyOfTenGgalStaysOpenAndReservesCash() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":10,"price":"800.00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.price").value("800.00"));

        mockMvc.perform(get(API_V1 + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("619500.00"))
                .andExpect(jsonPath("$.totalValue").value("889756.00"))
                .andExpect(jsonPath("$.positions[?(@.ticker == 'GGAL')]").isEmpty());
    }

    @Test
    void missingOrNonPositiveLimitPriceDoesNotInsertAnOrder() throws Exception {
        Integer before = jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);

        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":10}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":10,"price":"0"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":10,"price":"-1.00"}
                                """))
                .andExpect(status().isBadRequest());

        Integer after = jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);
        assertThat(after).isEqualTo(before);
    }

    @Test
    void limitAmountConvertsToWholeSharesAndReservesNotional() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","amount":"2500","price":"800.00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.price").value("800.00"));

        mockMvc.perform(get(API_V1 + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("625100.00"));
    }

    @Test
    void acceptedLimitIsNotFilledAgainstClose() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":10,"price":"800.00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.price").value("800.00"));
    }

    @Test
    void limitSellReservesSharesWithoutChangingCash() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"PAMP","side":"SELL","type":"LIMIT","size":10,"price":"1000.00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"));

        mockMvc.perform(get(API_V1 + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("627500.00"))
                .andExpect(jsonPath("$.totalValue").value("889756.00"))
                .andExpect(jsonPath("$.positions[?(@.ticker == 'PAMP')].quantity").value(40));

        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"PAMP","side":"SELL","type":"MARKET","size":40}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
