package com.cc.brokerage.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderQueryIT extends IntegrationTestBase {

    @Test
    void listsOrdersFromOldestToNewest() throws Exception {
        mockMvc.perform(get(API_V1 + "/accounts/10001/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(11))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[4].id").value(5))
                .andExpect(jsonPath("$[4].ticker").value("LOMA"))
                .andExpect(jsonPath("$[4].side").value("BUY"))
                .andExpect(jsonPath("$[4].type").value("LIMIT"))
                .andExpect(jsonPath("$[4].size").value(50))
                .andExpect(jsonPath("$[4].price").value("710.00"))
                .andExpect(jsonPath("$[4].status").value("NEW"))
                .andExpect(jsonPath("$[4].rejectionReason").doesNotExist())
                .andExpect(jsonPath("$[4].amount").doesNotExist())
                .andExpect(jsonPath("$[10].id").value(11));
    }

    @Test
    void getsAnOrderOfTheSameAccountWithoutTheRejectionReason() throws Exception {
        mockMvc.perform(get(API_V1 + "/accounts/10001/orders/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.ticker").value("LOMA"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.rejectionReason").doesNotExist())
                .andExpect(jsonPath("$.amount").doesNotExist());
    }

    @Test
    void otherAccountCannotReadTheOrder() throws Exception {
        mockMvc.perform(get(API_V1 + "/accounts/10002/orders/5"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BRK-001"))
                .andExpect(jsonPath("$.detail").value("The order does not belong to this account."));
    }

    @Test
    void missingOrderDoesNotEchoTheId() throws Exception {
        MvcResult result = mockMvc.perform(get(API_V1 + "/accounts/10001/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRK-003"))
                .andExpect(jsonPath("$.detail").value("Order not found."))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("999");
    }
}
