package com.cc.brokerage.UnitTest;

import com.cc.brokerage.adapter.out.persistence.InstrumentPersistenceAdapter;
import com.cc.brokerage.application.SearchInstrumentsService;
import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentQuote;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.domain.service.QuoteCalculator;
import com.cc.brokerage.support.Account10001Fixture;
import com.cc.brokerage.support.MoneyBounds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchInstrumentsServiceTest {

    @Mock
    private InstrumentRepository instruments;
    @Mock
    private MarketPriceRepository prices;

    private SearchInstrumentsService service;

    @BeforeEach
    void setUp() {
        service = new SearchInstrumentsService(instruments, prices, new QuoteCalculator(MoneyBounds.DEFAULT_MONEY));
    }

    @Test
    void searchesTrimmedTextWithoutTheCurrencyAndWithALimit() {
        when(instruments.search("banco", InstrumentType.MONEDA, SearchInstrumentsService.MAX_RESULTS)).thenReturn(List.of());

        assertThat(service.search("  banco  ")).isEmpty();
        verify(prices, never()).findLatestByInstrumentIds(any());
    }

    @Test
    void keepsTheRepositoryOrder() {
        when(instruments.search("banco", InstrumentType.MONEDA, SearchInstrumentsService.MAX_RESULTS)).thenReturn(List.of(
                new Instrument(22L, "BBAR", "Banco Frances", InstrumentType.ACCIONES),
                Account10001Fixture.BMA
        ));
        when(prices.findLatestByInstrumentIds(List.of(22L, 31L))).thenReturn(Map.of());

        assertThat(service.search("banco")).extracting(InstrumentQuote::ticker).containsExactly("BBAR", "BMA");
    }

    @Test
    void attachesLatestPriceAndDailyReturn() {
        stubSearch("pamp", Account10001Fixture.PAMP);
        when(prices.findLatestByInstrumentIds(List.of(47L)))
                .thenReturn(Map.of(47L, Account10001Fixture.latestPrices().get(Account10001Fixture.PAMP.id())));

        InstrumentQuote quote = service.search("pamp").getFirst();

        assertThat(quote.ticker()).isEqualTo("PAMP");
        assertThat(quote.lastPrice()).isEqualByComparingTo("925.85");
        assertThat(quote.dailyReturnPercent()).isEqualByComparingTo("0.44");
    }

    @Test
    void missingCloseLeavesPriceUnavailable() {
        Instrument instrument = new Instrument(99L, "XXXX", "Sin precio", InstrumentType.ACCIONES);
        stubSearch("xxxx", instrument);
        when(prices.findLatestByInstrumentIds(List.of(99L))).thenReturn(Map.of());

        InstrumentQuote quote = service.search("xxxx").getFirst();

        assertThat(quote.lastPrice()).isNull();
        assertThat(quote.dailyReturnPercent()).isNull();
    }

    @Test
    void zeroPreviousCloseLeavesDailyReturnUnavailable() {
        stubSearch("pamp", Account10001Fixture.PAMP);
        when(prices.findLatestByInstrumentIds(List.of(47L))).thenReturn(Map.of(47L,
                new MarketPrice(1L, 47L, null, null, null, new BigDecimal("10.00"), BigDecimal.ZERO, LocalDate.now())));

        assertThat(service.search("pamp").getFirst().dailyReturnPercent()).isNull();
    }

    @Test
    void blankQueryIsRejected() {
        assertThatThrownBy(() -> service.search("   "))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_EMPTY.message());
        assertThatThrownBy(() -> service.search(""))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_EMPTY.message());
        verifyNoInteractions(instruments);
    }

    @Test
    void queryAboveOneHundredCharactersIsRejected() {
        assertThatThrownBy(() -> service.search("n".repeat(101)))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_TOO_LONG.message());
    }

    @Test
    void hundredCharactersAreAccepted() {
        when(instruments.search(anyString(), any(), anyInt())).thenReturn(List.of());

        assertThat(service.search("n".repeat(100))).isEmpty();
    }

    @Test
    void controlCharacterIsRejected() {
        assertThatThrownBy(() -> service.search("pamp\n"))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_INVALID.message());
    }

    @Test
    void nullCharacterIsRejected() {
        assertThatThrownBy(() -> service.search("pamp\u0000"))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_INVALID.message());
    }

    @Test
    void likeWildcardsAreMatchedLiterally() {
        assertThat(InstrumentPersistenceAdapter.containsPattern("50%_off!")).isEqualTo("%50!%!_off!!%");
    }

    @Test
    void patternDoesNotDependOnMachineLocale() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            assertThat(InstrumentPersistenceAdapter.containsPattern("TIP")).isEqualTo("%tip%");
        } finally {
            Locale.setDefault(previous);
        }
    }

    private void stubSearch(String text, Instrument instrument) {
        when(instruments.search(text, InstrumentType.MONEDA, SearchInstrumentsService.MAX_RESULTS)).thenReturn(List.of(instrument));
    }
}
