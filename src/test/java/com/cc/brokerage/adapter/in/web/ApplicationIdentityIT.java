package com.cc.brokerage.adapter.in.web;

import com.cc.brokerage.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationIdentityIT extends IntegrationTestBase {

    @Autowired
    private Environment environment;

    @Test
    void applicationNameIsBrokerageApi() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("brokerage-api");
    }

    @Test
    void bannerIdentifiesTheService() throws Exception {
        String banner = new String(new ClassPathResource("banner.txt").getInputStream().readAllBytes());
        assertThat(banner).contains("brokerage-api");
    }

    @Test
    void operationsWithoutBasePathOrVersionAreNotServed() throws Exception {
        mockMvc.perform(get("/api/accounts/10001/portfolio"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE_PATH + "/accounts/10001/portfolio"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/instruments").param("q", "pamp"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/accounts/10001/orders")
                        .contentType("application/json")
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"MARKET","size":1}
                                """))
                .andExpect(status().isNotFound());
    }
}
