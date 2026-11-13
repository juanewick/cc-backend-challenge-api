package com.cc.brokerage.IntegrationTest;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Order(2)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GeneratedOpenApiIT extends IntegrationTestBase {

    @Test
    @Order(2)
    void generatedDocumentListsTheBusinessOperations() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/brokerage-api/v1/accounts/{accountNumber}/portfolio']").exists())
                .andExpect(jsonPath("$.paths['/api/brokerage-api/v1/instruments']").exists())
                .andExpect(jsonPath("$.paths['/api/brokerage-api/v1/instruments/{ticker}']").exists())
                .andExpect(jsonPath("$.paths['/api/brokerage-api/v1/accounts/{accountNumber}/orders']").exists())
                .andExpect(jsonPath("$.paths['/api/brokerage-api/v1/accounts/{accountNumber}/orders/{orderId}']").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(document).contains("0,48 %");
        assertThat(document).contains("settledCash");
        assertThat(document).contains("\"400\"");
        assertThat(document).contains("\"404\"");
        assertThat(document).contains("\"409\"");
        assertThat(document).contains("/v3/api-docs#/components/schemas/InvalidParameters");
        assertThat(document).contains("invalid_params");
        assertThat(document).contains("application/problem+json");
    }
}
