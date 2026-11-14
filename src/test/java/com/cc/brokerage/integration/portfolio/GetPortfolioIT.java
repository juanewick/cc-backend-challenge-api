package com.cc.brokerage.integration.portfolio;

import com.cc.brokerage.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GetPortfolioIT extends IntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void account10001MatchesOracle() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("10001"))
                .andExpect(jsonPath("$.settledCash").value("753000.00"))
                .andExpect(jsonPath("$.reservedCash").value("125500.00"))
                .andExpect(jsonPath("$.availableCash").value("627500.00"))
                .andExpect(jsonPath("$.totalValue").value("889756.00"))
                .andExpect(jsonPath("$.positions[0].ticker").value("BMA"))
                .andExpect(jsonPath("$.positions[0].quantity").value(-10))
                .andExpect(jsonPath("$.positions[0].marketValue").value("-15028.00"))
                .andExpect(jsonPath("$.positions[0].totalReturnPercent").value("1.78"))
                .andExpect(jsonPath("$.positions[0].dailyReturnPercent").value("-1.15"))
                .andExpect(jsonPath("$.positions[1].ticker").value("METR"))
                .andExpect(jsonPath("$.positions[1].quantity").value(500))
                .andExpect(jsonPath("$.positions[1].marketValue").value("114750.00"))
                .andExpect(jsonPath("$.positions[1].totalReturnPercent").value("-8.20"))
                .andExpect(jsonPath("$.positions[1].dailyReturnPercent").value("-1.08"))
                .andExpect(jsonPath("$.positions[2].ticker").value("PAMP"))
                .andExpect(jsonPath("$.positions[2].quantity").value(40))
                .andExpect(jsonPath("$.positions[2].marketValue").value("37034.00"))
                .andExpect(jsonPath("$.positions[2].totalReturnPercent").value("-0.45"))
                .andExpect(jsonPath("$.positions[2].dailyReturnPercent").value("0.44"))
                .andExpect(jsonPath("$.unpricedTickers").isEmpty());
    }

    @Test
    void accountWithoutOrdersIsZero() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/10002/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settledCash").value("0.00"))
                .andExpect(jsonPath("$.reservedCash").value("0.00"))
                .andExpect(jsonPath("$.availableCash").value("0.00"))
                .andExpect(jsonPath("$.totalValue").value("0.00"))
                .andExpect(jsonPath("$.positions").isEmpty());
    }

    @Test
    void unknownAccountIsNotFound() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/99999/portfolio"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRK-004"))
                .andExpect(jsonPath("$.detail").value("Account not found."));
    }

    @Test
    void unknownAccountDoesNotEchoTheNumber() throws Exception {
        MvcResult result = mockMvc.perform(get(apiV1() + "/accounts/{accountNumber}/portfolio", "99999"))
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

    @ParameterizedTest
    @ValueSource(strings = {"ABCDE", "10001 ", " 10001", "10001' OR '1'='1", "123456789012345678901"})
    void invalidAccountIsRejectedWithoutEchoingIt(String account) throws Exception {
        MvcResult portfolio = mockMvc.perform(get(apiV1() + "/accounts/{accountNumber}/portfolio", account))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-008"))
                .andExpect(jsonPath("$.detail").value("The account is not valid."))
                .andReturn();

        assertThat(portfolio.getResponse().getContentAsString()).doesNotContain(account);
        assertThat(portfolio.getResponse().getContentAsString()).doesNotContain("627500.00");
    }

    @Test
    void ordersWithASideOrStatusUnknownToTheServiceAreLeftOutOfThePortfolio() throws Exception {
        jdbc.update("INSERT INTO orders (instrumentId, userId, size, price, side, status, type, datetime) "
                + "VALUES (47, 1, 7, 900, 'DIVIDEND', 'FILLED', 'MARKET', '2023-07-14 10:00:00')");
        jdbc.update("INSERT INTO orders (instrumentId, userId, size, price, side, status, type, datetime) "
                + "VALUES (47, 1, 7, 900, 'BUY', 'PARTIAL', 'MARKET', '2023-07-14 10:01:00')");

        mockMvc.perform(get(apiV1() + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("627500.00"))
                .andExpect(jsonPath("$.totalValue").value("889756.00"));
    }
}
