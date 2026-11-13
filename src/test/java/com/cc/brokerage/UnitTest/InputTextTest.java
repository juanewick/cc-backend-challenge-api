package com.cc.brokerage.UnitTest;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.InputText;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputTextTest {

    @Test
    void accountKeepsLeadingZerosAndRejectsAnythingElse() {
        assertThat(InputText.account("10001")).isEqualTo("10001");
        assertThat(InputText.account("00001")).isEqualTo("00001");
        assertThat(InputText.account("1")).isEqualTo("1");
        assertThat(InputText.account("1".repeat(20))).isEqualTo("1".repeat(20));

        assertInvalidAccount(" 10001");
        assertInvalidAccount("10001 ");
        assertInvalidAccount("");
        assertInvalidAccount("ABCDE");
        assertInvalidAccount("10001' OR '1'='1");
        assertInvalidAccount("1".repeat(21));
        assertInvalidAccount(null);
    }

    @Test
    void tickerKeepsCaseAndRejectsSpacesAndSigns() {
        assertThat(InputText.ticker("PAMP")).isEqualTo("PAMP");
        assertThat(InputText.ticker("pamp")).isEqualTo("pamp");
        assertThat(InputText.ticker("GgAl1")).isEqualTo("GgAl1");
        assertThat(InputText.ticker("A".repeat(10))).isEqualTo("A".repeat(10));

        assertInvalidTicker(" PAMP");
        assertInvalidTicker("PAMP ");
        assertInvalidTicker("");
        assertInvalidTicker("PAMP-1");
        assertInvalidTicker("ABCDEFGHIJK");
        assertInvalidTicker("PAMP'; DROP TABLE orders;--");
        assertInvalidTicker(null);
    }

    @Test
    void searchTrimsOnlyTheEnds() {
        assertThat(InputText.search("  pamp  ")).isEqualTo("pamp");
        assertThat(InputText.search("Banco Hipotecario S.A.")).isEqualTo("Banco Hipotecario S.A.");
        assertThat(InputText.search(" " + "n".repeat(100))).isEqualTo("n".repeat(100));
        assertThat(InputText.search("á".repeat(100))).hasSize(100);

        assertThatThrownBy(() -> InputText.search("   "))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_EMPTY.message());
        assertThatThrownBy(() -> InputText.search(""))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_EMPTY.message());
        assertThatThrownBy(() -> InputText.search(null))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_EMPTY.message());
        assertThatThrownBy(() -> InputText.search("n".repeat(101)))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_TOO_LONG.message());
        assertThatThrownBy(() -> InputText.search(" pamp\n "))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_INVALID.message());
        assertThatThrownBy(() -> InputText.search("pamp\u0000"))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_INVALID.message());
    }

    private static void assertInvalidAccount(String value) {
        assertThatThrownBy(() -> InputText.account(value))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.INVALID_ACCOUNT.message());
    }

    private static void assertInvalidTicker(String value) {
        assertThatThrownBy(() -> InputText.ticker(value))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.INVALID_TICKER.message());
    }
}
