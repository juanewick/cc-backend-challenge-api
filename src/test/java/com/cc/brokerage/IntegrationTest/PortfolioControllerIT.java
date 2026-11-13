package com.cc.brokerage.IntegrationTest;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PortfolioControllerIT extends IntegrationTestBase {

    @Test
    void account10001MatchesOracle() throws Exception {
        mockMvc.perform(get(API_V1 + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("10001"))
                .andExpect(jsonPath("$.settledCash").value("753000.00"))
                .andExpect(jsonPath("$.reservedCash").value("125500.00"))
                .andExpect(jsonPath("$.availableCash").value("627500.00"))
                .andExpect(jsonPath("$.totalValue").value("889756.00"))
                .andExpect(jsonPath("$.positions[0].ticker").value("BMA"))
                .andExpect(jsonPath("$.positions[0].quantity").value(-10))
                .andExpect(jsonPath("$.positions[0].marketValue").value("-15028.00"))
                .andExpect(jsonPath("$.positions[0].totalReturnPercent").value("0.48"))
                .andExpect(jsonPath("$.positions[0].dailyReturnPercent").value("-1.15"))
                .andExpect(jsonPath("$.positions[1].ticker").value("METR"))
                .andExpect(jsonPath("$.positions[1].quantity").value(500))
                .andExpect(jsonPath("$.positions[1].marketValue").value("114750.00"))
                .andExpect(jsonPath("$.positions[1].totalReturnPercent").value("-8.20"))
                .andExpect(jsonPath("$.positions[1].dailyReturnPercent").value("-1.08"))
                .andExpect(jsonPath("$.positions[2].ticker").value("PAMP"))
                .andExpect(jsonPath("$.positions[2].quantity").value(40))
                .andExpect(jsonPath("$.positions[2].marketValue").value("37034.00"))
                .andExpect(jsonPath("$.positions[2].totalReturnPercent").value("-0.18"))
                .andExpect(jsonPath("$.positions[2].dailyReturnPercent").value("0.44"));
    }

    @Test
    void accountWithoutOrdersIsZero() throws Exception {
        mockMvc.perform(get(API_V1 + "/accounts/10002/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settledCash").value("0.00"))
                .andExpect(jsonPath("$.reservedCash").value("0.00"))
                .andExpect(jsonPath("$.availableCash").value("0.00"))
                .andExpect(jsonPath("$.totalValue").value("0.00"))
                .andExpect(jsonPath("$.positions").isEmpty());
    }

    @Test
    void unknownAccountIsNotFound() throws Exception {
        mockMvc.perform(get(API_V1 + "/accounts/99999/portfolio"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRK-004"))
                .andExpect(jsonPath("$.detail").value("Account not found."));
    }
}
