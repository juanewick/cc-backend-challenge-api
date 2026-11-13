package com.cc.brokerage.IntegrationTest;

import com.cc.brokerage.domain.model.MoneyMath;
import com.cc.brokerage.support.MoneyBounds;
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

    private String orders() {
        return apiV1() + "/accounts/10001/orders";
    }

    private static final String INVALID_ORDER = "The order is not valid.";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MoneyMath money;

    private MoneyBounds bounds() {
        return new MoneyBounds(money);
    }

    @Test
    void zeroSizeIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":%d,"price":"1.00"}
                """.formatted(money.minShares() - 1));
    }

    @Test
    void negativeSizeIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":-1,"price":"1.00"}
                """);
    }

    @Test
    void sizeAboveTheMaximumIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":%d,"price":"1.00"}
                """.formatted(money.maxShares() + 1));
    }

    @Test
    void maximumSizeIsNotRejectedForSize() throws Exception {
        Integer before = orderCount();

        mockMvc.perform(post(orders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":%d,"price":"0.01"}
                                """.formatted(money.maxShares())))
                .andExpect(status().isCreated());

        assertThat(orderCount()).isEqualTo(before + 1);
    }

    @Test
    void negativeAmountIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"MARKET","amount":"%s"}
                """.formatted(bounds().belowMinAmount().toPlainString()));
    }

    @Test
    void nonNumericAmountIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"MARKET","amount":"not-an-amount"}
                """, "BRK-020", "amount");
    }

    @Test
    void emptyAmountIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"MARKET","amount":""}
                """, "BRK-020", "amount");
    }

    @Test
    void amountThatRoundsPastTheMaximumIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"MARKET","amount":"%s"}
                """.formatted(bounds().justPastMax().toPlainString()));
    }

    @Test
    void negativePriceIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"%s"}
                """.formatted(bounds().belowMinAmount().toPlainString()));
    }

    @Test
    void nonNumericPriceIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"not-a-price"}
                """, "BRK-023", "price");
    }

    @Test
    void priceThatRoundsPastTheMaximumIsRejectedWithoutSaving() throws Exception {
        rejectOrder("""
                {"ticker":"GGAL","side":"BUY","type":"LIMIT","size":1,"price":"%s"}
                """.formatted(bounds().justPastMax().toPlainString()));
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

    private void rejectOrder(String body) throws Exception {
        Integer before = orderCount();

        mockMvc.perform(post(orders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(orderCount()).isEqualTo(before);
    }

    private void rejectOrder(String body, String code, String field) throws Exception {
        Integer before = orderCount();

        MvcResult result = mockMvc.perform(post(orders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.invalid_params[0].field").value(field))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("not-a");
        assertThat(orderCount()).isEqualTo(before);
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
