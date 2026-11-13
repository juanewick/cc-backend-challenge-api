package com.cc.brokerage.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaceMarketOrderIT extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void marketBuyOfOneGgalIsFilledAndUpdatesPortfolio() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"MARKET","size":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/brokerage-api/v1/accounts/10001/orders/")))
                .andExpect(jsonPath("$.ticker").value("GGAL"))
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.price").value("885.80"))
                .andExpect(jsonPath("$.amount").value("885.80"))
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());

        mockMvc.perform(get(API_V1 + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("626614.20"))
                .andExpect(jsonPath("$.positions[?(@.ticker == 'GGAL')].quantity").value(1));
    }

    @Test
    void amountConvertsToWholeShares() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"MARKET","amount":"2000"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.price").value("885.80"))
                .andExpect(jsonPath("$.status").value("FILLED"));
    }

    @Test
    void invalidPayloadDoesNotInsertAnOrder() throws Exception {
        Integer before = jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);

        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"MARKET","size":1,"amount":"2000"}
                                """))
                .andExpect(status().isBadRequest());

        Integer after = jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);
        assertThat(after).isEqualTo(before);
    }

    @Test
    void unknownAccountIsNotFound() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/99999/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"MARKET","size":1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    void lowercaseTickerUsesCatalogSymbol() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"ggal","side":"BUY","type":"MARKET","size":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("GGAL"))
                .andExpect(jsonPath("$.price").value("885.80"))
                .andExpect(jsonPath("$.status").value("FILLED"));
    }

    @Test
    void marketBuyOfOnePampUsesTheSameCloseAsTheQuote() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"PAMP","side":"BUY","type":"MARKET","size":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("PAMP"))
                .andExpect(jsonPath("$.price").value("925.85"))
                .andExpect(jsonPath("$.amount").value("925.85"))
                .andExpect(jsonPath("$.status").value("FILLED"));
    }
}
