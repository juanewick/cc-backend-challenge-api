package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CancelOrderIT extends IntegrationTestBase {

    @Test
    void cancellingSeedLomaOrderReleasesCash() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders/5/cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get(API_V1 + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("663000.00"));
    }

    @Test
    void filledOrderCannotBeCancelled() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders/2/cancellation"))
                .andExpect(status().isConflict());
    }

    @Test
    void otherAccountCannotCancel() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10002/orders/5/cancellation"))
                .andExpect(status().isConflict());
    }

    @Test
    void missingOrderIsNotFound() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders/999/cancellation"))
                .andExpect(status().isNotFound());
    }
}
