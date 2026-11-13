package com.cc.brokerage.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HardenNumbersIT extends IntegrationTestBase {

    private static final String ORDERS = API_V1 + "/accounts/10001/orders";
    private static final String INVALID_ORDER = "La orden no es válida";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void zeroSizeIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":0,"price":"1.00"}
                """);
    }

    @Test
    void negativeSizeIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":-1,"price":"1.00"}
                """);
    }

    @Test
    void sizeAboveIntegerMaxIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":2147483648,"price":"1.00"}
                """);
    }

    @Test
    void maxIntegerSizeIsNotRejectedForSize() throws Exception {
        Integer before = orderCount();

        mockMvc.perform(post(ORDERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":2147483647,"price":"0.01"}
                                """))
                .andExpect(status().isCreated());

        assertThat(orderCount()).isEqualTo(before + 1);
    }

    @Test
    void negativeAmountIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"MARKET","amount":"-1"}
                """);
    }

    @Test
    void nonNumericAmountIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"MARKET","amount":"no-es-monto"}
                """);
    }

    @Test
    void amountThatRoundsPastEightIntegerDigitsIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"MARKET","amount":"99999999.995"}
                """);
    }

    @Test
    void negativePriceIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"-1"}
                """);
    }

    @Test
    void nonNumericPriceIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"no-es-precio"}
                """);
    }

    @Test
    void priceThatRoundsPastEightIntegerDigitsIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"99999999.995"}
                """);
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
        MvcResult result = mockMvc.perform(post(ORDERS + "/999/cancellation"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("La orden no existe"))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("999");
        assertThat(statusOf(5L)).isEqualTo("NEW");
    }

    private void rejectOrder(String body) throws Exception {
        Integer before = orderCount();

        mockMvc.perform(post(ORDERS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(orderCount()).isEqualTo(before);
    }

    private void rejectCancellation(String orderId) throws Exception {
        Integer before = orderCount();

        mockMvc.perform(post(ORDERS + "/" + orderId + "/cancellation"))
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
