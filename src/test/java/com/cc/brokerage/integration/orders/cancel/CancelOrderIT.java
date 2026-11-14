package com.cc.brokerage.integration.orders.cancel;

import com.cc.brokerage.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CancelOrderIT extends IntegrationTestBase {

    private static final String INVALID_ORDER = "The order is not valid.";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String orders() {
        return apiV1() + "/accounts/10001/orders";
    }

    @Test
    void cancellingSeedLomaOrderReleasesCash() throws Exception {
        mockMvc.perform(post(apiV1() + "/accounts/10001/orders/5/cancellation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("LOMA"))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.rejectionReason").doesNotExist());

        mockMvc.perform(get(apiV1() + "/accounts/10001/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCash").value("663000.00"));
    }

    @Test
    void filledOrderCannotBeCancelled() throws Exception {
        mockMvc.perform(post(apiV1() + "/accounts/10001/orders/2/cancellation"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/v3/api-docs#/components/schemas/Conflict"))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("BRK-005"))
                .andExpect(jsonPath("$.detail").value("Only open orders can be cancelled."))
                .andExpect(jsonPath("$.invalid_params").doesNotExist());
    }

    @Test
    void orderOfAnotherAccountIsNotFound() throws Exception {
        mockMvc.perform(post(apiV1() + "/accounts/10002/orders/5/cancellation"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRK-003"))
                .andExpect(jsonPath("$.detail").value("Order not found."));
    }

    @Test
    void missingOrderIsNotFound() throws Exception {
        mockMvc.perform(post(apiV1() + "/accounts/10001/orders/999/cancellation"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRK-003"))
                .andExpect(jsonPath("$.detail").value("Order not found."));
    }

    @Test
    void zeroOrderIdIsRejectedAndCancelsNothing() throws Exception {
        rejectCancellation("0");
    }

    @Test
    void nonIntegerOrderIdIsRejectedAndCancelsNothing() throws Exception {
        rejectCancellation("abc");
    }

    @Test
    void orderIdAboveIntegerMaxIsRejectedAndCancelsNothing() throws Exception {
        rejectCancellation("2147483648");
    }

    @Test
    void missingOrderDoesNotEchoTheId() throws Exception {
        MvcResult result = mockMvc.perform(post(orders() + "/999/cancellation"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order not found."))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("999");
        assertThat(statusOf(5L)).isEqualTo("NEW");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABCDE", "10001 ", " 10001", "10001' OR '1'='1", "123456789012345678901"})
    void invalidAccountIsRejectedAndCancelsNothing(String account) throws Exception {
        Integer before = orderCount();

        mockMvc.perform(post(apiV1() + "/accounts/{accountNumber}/orders/5/cancellation", account))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRK-008"))
                .andExpect(jsonPath("$.detail").value("The account is not valid."));

        assertThat(orderCount()).isEqualTo(before);
        assertThat(statusOf(5L)).isEqualTo("NEW");
    }

    private void rejectCancellation(String orderId) throws Exception {
        Integer before = orderCount();

        mockMvc.perform(post(orders() + "/" + orderId + "/cancellation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(INVALID_ORDER));

        assertThat(orderCount()).isEqualTo(before);
        assertThat(statusOf(5L)).isEqualTo("NEW");
    }

    private Integer orderCount() {
        return jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);
    }

    private String statusOf(long orderId) {
        return jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, orderId);
    }
}
