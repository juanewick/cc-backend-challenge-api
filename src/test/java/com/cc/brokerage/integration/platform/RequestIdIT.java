package com.cc.brokerage.integration.platform;

import com.cc.brokerage.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RequestIdIT extends IntegrationTestBase {

    private static final String HEADER = "X-Request-Id";

    @Test
    void everyApiResponseCarriesARequestId() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(header().string(HEADER, matchesPattern("[0-9a-f]{8}")));
    }

    @Test
    void errorResponsesCarryItToo() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/99999/portfolio"))
                .andExpect(status().isNotFound())
                .andExpect(header().exists(HEADER));
    }

    @Test
    void theCallersRequestIdIsKept() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/10001/portfolio").header(HEADER, "client-abc_123"))
                .andExpect(header().string(HEADER, "client-abc_123"));
    }

    @Test
    void aRequestIdThatIsNotPlainTextIsReplaced() throws Exception {
        mockMvc.perform(get(apiV1() + "/accounts/10001/portfolio").header(HEADER, "two words\tand a tab"))
                .andExpect(header().string(HEADER, matchesPattern("[0-9a-f]{8}")));
    }

    @Test
    void statusEndpointsAreLeftAlone() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HEADER));
    }
}
