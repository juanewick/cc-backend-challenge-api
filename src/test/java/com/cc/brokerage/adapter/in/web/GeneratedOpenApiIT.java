package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.IntegrationTestBase;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Order(2)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GeneratedOpenApiIT extends IntegrationTestBase {

    @Test
    @Order(2)
    void generatedDocumentListsTheBusinessOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/brokerage-api/v1/accounts/{accountNumber}/portfolio']").exists())
                .andExpect(jsonPath("$.paths['/api/brokerage-api/v1/instruments']").exists())
                .andExpect(jsonPath("$.paths['/api/brokerage-api/v1/accounts/{accountNumber}/orders']").exists());
    }
}
