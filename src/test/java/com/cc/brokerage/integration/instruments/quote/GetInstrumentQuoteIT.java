package com.cc.brokerage.integration.instruments.quote;

import com.cc.brokerage.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GetInstrumentQuoteIT extends IntegrationTestBase {

    @Test
    void quoteByTickerUsesTheLatestClose() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments/PAMP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("PAMP"))
                .andExpect(jsonPath("$.lastPrice").value("925.85"))
                .andExpect(jsonPath("$.dailyReturnPercent").value("0.44"));
    }

    @Test
    void unknownTickerQuoteIsNotFound() throws Exception {
        var missing = mockMvc.perform(get(apiV1() + "/instruments/ZZZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRK-002"))
                .andExpect(jsonPath("$.detail").value("Instrument not found."))
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(missing.getResponse().getContentAsString()).doesNotContain("ZZZZ");
    }

    @Test
    void currencyQuoteIsNotTradable() throws Exception {
        var currency = mockMvc.perform(get(apiV1() + "/instruments/ARS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/v3/api-docs#/components/schemas/BadRequest"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("BRK-006"))
                .andExpect(jsonPath("$.detail").value("The currency is not tradable."))
                .andExpect(jsonPath("$.invalid_params").doesNotExist())
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(currency.getResponse().getContentAsString()).doesNotContain("ARS");
    }
}
