package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InstrumentControllerIT extends IntegrationTestBase {

    @Test
    void bancoReturnsFourBanksInTickerOrder() throws Exception {
        mockMvc.perform(get(API_V1 + "/instruments").param("q", "banco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].ticker").value("BBAR"))
                .andExpect(jsonPath("$[1].ticker").value("BHIP"))
                .andExpect(jsonPath("$[2].ticker").value("BMA"))
                .andExpect(jsonPath("$[3].ticker").value("BPAT"));
    }

    @Test
    void pampReturnsSingleQuoteWithPriceAndDailyReturn() throws Exception {
        mockMvc.perform(get(API_V1 + "/instruments").param("q", "pamp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticker").value("PAMP"))
                .andExpect(jsonPath("$[0].lastPrice").value("925.85"))
                .andExpect(jsonPath("$[0].dailyReturnPercent").value("0.44"));
    }

    @Test
    void ypfMatchesTickerSubstring() throws Exception {
        mockMvc.perform(get(API_V1 + "/instruments").param("q", "YPF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ticker == 'YPFD')]").exists());
    }

    @Test
    void unknownTextReturnsEmptyList() throws Exception {
        mockMvc.perform(get(API_V1 + "/instruments").param("q", "zzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void pesosAndArsAreNotTradable() throws Exception {
        mockMvc.perform(get(API_V1 + "/instruments").param("q", "PESOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(API_V1 + "/instruments").param("q", "ARS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        var currency = mockMvc.perform(get(API_V1 + "/instruments/ARS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/v3/api-docs#/components/schemas/BadRequest"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INSTRUMENT_NOT_TRADABLE"))
                .andExpect(jsonPath("$.detail").value("La moneda no es operable"))
                .andExpect(jsonPath("$.invalid_params").doesNotExist())
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(currency.getResponse().getContentAsString()).doesNotContain("ARS");
    }

    @Test
    void quoteByTickerUsesTheLatestClose() throws Exception {
        mockMvc.perform(get(API_V1 + "/instruments/PAMP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("PAMP"))
                .andExpect(jsonPath("$.lastPrice").value("925.85"))
                .andExpect(jsonPath("$.dailyReturnPercent").value("0.44"));
    }

    @Test
    void unknownTickerQuoteIsNotFound() throws Exception {
        var missing = mockMvc.perform(get(API_V1 + "/instruments/ZZZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INSTRUMENT_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("El activo no existe"))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(missing.getResponse().getContentAsString()).doesNotContain("ZZZZ");
    }

    @Test
    void blankQueryIsRejected() throws Exception {
        mockMvc.perform(get(API_V1 + "/instruments").param("q", "   "))
                .andExpect(status().isBadRequest());
    }
}
