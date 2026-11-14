package com.cc.brokerage.integration.orders.get;

import com.cc.brokerage.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GetOrderIT extends IntegrationTestBase {

    @Test
    void getsAnOrderOfTheSameAccountWithoutTheRejectionReason() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/10001/orders/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.ticker").value("LOMA"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.rejectionReason").doesNotExist())
                .andExpect(jsonPath("$.amount").doesNotExist());
    }

    @Test
    void orderOfAnotherAccountIsNotFound() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/10002/orders/5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRK-003"))
                .andExpect(jsonPath("$.detail").value("Order not found."));
    }

    @Test
    void missingOrderDoesNotEchoTheId() throws Exception {
        MvcResult result = mockMvc.perform(get(apiV1() + "/accounts/10001/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRK-003"))
                .andExpect(jsonPath("$.detail").value("Order not found."))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("999");
    }
}
