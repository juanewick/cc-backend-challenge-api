package com.cc.brokerage.IntegrationTest;

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
                .andExpect(jsonPath("$.ticker").value("LOMA"))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());

        mockMvc.perform(get(API_V1 + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("663000.00"));
    }

    @Test
    void filledOrderCannotBeCancelled() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders/2/cancellation"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/v3/api-docs#/components/schemas/Conflict"))
                .andExpect(jsonPath("$.title").value("Conflicto"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("ORDER_NOT_OPEN"))
                .andExpect(jsonPath("$.detail").value("Solo se cancelan órdenes abiertas"))
                .andExpect(jsonPath("$.invalid_params").doesNotExist());
    }

    @Test
    void otherAccountCannotCancel() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10002/orders/5/cancellation"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_OWNED"))
                .andExpect(jsonPath("$.detail").value("La orden no pertenece a esa cuenta"));
    }

    @Test
    void missingOrderIsNotFound() throws Exception {
        mockMvc.perform(post(API_V1 + "/accounts/10001/orders/999/cancellation"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("La orden no existe"));
    }
}
