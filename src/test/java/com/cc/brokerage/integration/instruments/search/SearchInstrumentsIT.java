package com.cc.brokerage.integration.instruments.search;

import com.cc.brokerage.integration.IntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchInstrumentsIT extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void bancoReturnsFourBanksInTickerOrder() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "banco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(4))
                .andExpect(jsonPath("$.content[0].ticker").value("BBAR"))
                .andExpect(jsonPath("$.content[1].ticker").value("BHIP"))
                .andExpect(jsonPath("$.content[2].ticker").value("BMA"))
                .andExpect(jsonPath("$.content[3].ticker").value("BPAT"));
    }

    @Test
    void pampReturnsSingleQuoteWithPriceAndDailyReturn() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "pamp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].ticker").value("PAMP"))
                .andExpect(jsonPath("$.content[0].lastPrice").value("925.85"))
                .andExpect(jsonPath("$.content[0].dailyReturnPercent").value("0.44"));
    }

    @Test
    void ypfMatchesTickerSubstring() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "YPF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.ticker == 'YPFD')]").exists());
    }

    @Test
    void unknownTextReturnsAnEmptyPage() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "zzzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void currencyIsNotASearchResult() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "PESOS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "ARS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void searchIgnoresCaseAndSurroundingSpaces() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "  BaNcO  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(4));
    }

    @Test
    void likeWildcardsAndQuotesAreMatchedLiterally() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "pamp' OR 1=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void searchAnswersTheFirstPageWithTheTotals() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "banco"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void broadSearchIsPagedAndSorted() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.content[0].ticker").value("AGRO"))
                .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThan(50)));
    }

    @Test
    void walkingEveryPageOfABroadSearchReturnsEachMatchOnceInTickerOrder() throws Exception {
        List<String> expected = jdbc.queryForList("SELECT ticker FROM instruments WHERE type <> 'MONEDA' "
                + "AND (lower(ticker) LIKE '%a%' OR lower(name) LIKE '%a%') ORDER BY ticker, id", String.class);
        int pageSize = 7;
        List<String> tickers = new ArrayList<>();
        int totalPages = Integer.MAX_VALUE;
        for (int page = 0; page < totalPages; page++) {
            String body = mockMvc.perform(get(apiV1() + "/instruments").param("q", "a")
                            .param("page", String.valueOf(page)).param("pageSize", String.valueOf(pageSize)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(page))
                    .andExpect(jsonPath("$.totalElements").value(expected.size()))
                    .andReturn().getResponse().getContentAsString();
            totalPages = JsonPath.read(body, "$.totalPages");
            List<String> content = JsonPath.read(body, "$.content[*].ticker");
            assertThat(content).hasSize(page < totalPages - 1 ? pageSize : expected.size() - pageSize * (totalPages - 1));
            tickers.addAll(content);
        }

        assertThat(totalPages).isEqualTo((expected.size() + pageSize - 1) / pageSize);
        assertThat(tickers).containsExactlyElementsOf(expected);
    }

    @Test
    void pagesKeepTheTickerOrderAcrossRequests() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "banco").param("page", "0").param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].ticker").value("BBAR"))
                .andExpect(jsonPath("$.content[2].ticker").value("BMA"))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "banco").param("page", "1").param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].ticker").value("BPAT"))
                .andExpect(jsonPath("$.page").value(1));
    }

    @Test
    void pageBeyondTheLastIsEmptyButKeepsTheTotal() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "banco").param("page", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void invalidPageParametersAreRejected() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "banco").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-025"));
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "banco").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-026"));
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "banco").param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-026"));
    }

    @Test
    void blankQueryIsRejected() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankQueryIsReportedBeforeAnInvalidPage() throws Exception {
        mockMvc.perform(get(apiV1() + "/instruments").param("q", "   ").param("page", "-1").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-011"));
    }

    @Test
    void instrumentTypeUnknownToTheServiceDoesNotBreakTheSearch() throws Exception {
        jdbc.update("INSERT INTO instruments (ticker, name, type) VALUES ('ZZBOND', 'Test bond', 'BONOS')");
        try {
            mockMvc.perform(get(apiV1() + "/instruments").param("q", "ZZBOND"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].ticker").value("ZZBOND"))
                    .andExpect(jsonPath("$.content[0].type").value("UNKNOWN"));
        } finally {
            jdbc.update("DELETE FROM instruments WHERE ticker = 'ZZBOND'");
        }
    }
}
