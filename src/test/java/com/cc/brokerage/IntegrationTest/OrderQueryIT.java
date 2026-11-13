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
        mockMvc.perform(get(apiV1() + "/accounts/10001/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(11))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[4].id").value(5))
                .andExpect(jsonPath("$.content[4].ticker").value("LOMA"))
                .andExpect(jsonPath("$.content[4].side").value("BUY"))
                .andExpect(jsonPath("$.content[4].type").value("LIMIT"))
                .andExpect(jsonPath("$.content[4].size").value(50))
                .andExpect(jsonPath("$.content[4].price").value("710.00"))
                .andExpect(jsonPath("$.content[4].status").value("NEW"))
                .andExpect(jsonPath("$.content[4].rejectionReason").doesNotExist())
                .andExpect(jsonPath("$.content[4].amount").doesNotExist())
                .andExpect(jsonPath("$.content[10].id").value(11));
    }

    @Test
    void pagesKeepTheOrderAcrossRequests() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/10001/orders").param("page", "1").param("pageSize", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(4))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[3].id").value(8))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(4))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(3));

        mockMvc.perform(get(apiV1() + "/accounts/10001/orders").param("page", "5").param("pageSize", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(11));
    }

    @Test
    void filtersByStatus() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/10001/orders").param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.content[1].id").value(7));
    }

    @Test
    void invalidListParametersAreRejected() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/10001/orders").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-024"))
                .andExpect(jsonPath("$.invalid_params[0].field").value("status"));
        mockMvc.perform(get(apiV1() + "/accounts/10001/orders").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-025"))
                .andExpect(jsonPath("$.invalid_params[0].field").value("page"));
        mockMvc.perform(get(apiV1() + "/accounts/10001/orders").param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-026"))
                .andExpect(jsonPath("$.detail").value("Page size must be an integer from 1 to 100."))
                .andExpect(jsonPath("$.invalid_params[0].field").value("pageSize"));
        mockMvc.perform(get(apiV1() + "/accounts/10001/orders").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-026"));
    }

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
