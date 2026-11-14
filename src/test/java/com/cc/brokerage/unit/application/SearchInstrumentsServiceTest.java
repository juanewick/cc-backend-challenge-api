package com.cc.brokerage.unit.application;

import com.cc.brokerage.adapter.out.persistence.InstrumentPersistenceAdapter;
import com.cc.brokerage.application.SearchInstrumentsService;
import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentQuote;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.model.PageQuery;
import com.cc.brokerage.domain.model.PageResult;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchInstrumentsServiceTest {

    private static final PageQuery PAGE = PageQuery.first();

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
    void searchesTrimmedTextWithoutTheCurrencyOnTheRequestedPage() {
        PageQuery second = new PageQuery(1, 5);
        when(instruments.search("banco", InstrumentType.MONEDA, second)).thenReturn(new PageResult<>(List.of(), 1, 5, 4));

        PageResult<InstrumentQuote> found = service.search("  banco  ", second);

        assertThat(found.content()).isEmpty();
        assertThat(found.page()).isEqualTo(1);
        assertThat(found.size()).isEqualTo(5);
        assertThat(found.totalElements()).isEqualTo(4);
        verify(prices, never()).findLatestByInstrumentIds(any());
    }

    @Test
    void keepsTheRepositoryOrder() {
        when(instruments.search("banco", InstrumentType.MONEDA, PAGE)).thenReturn(page(
                new Instrument(22L, "BBAR", "Banco Frances", InstrumentType.ACCIONES),
                Account10001Fixture.BMA
        ));
        when(prices.findLatestByInstrumentIds(List.of(22L, 31L))).thenReturn(Map.of());

        assertThat(service.search("banco", PAGE).content()).extracting(InstrumentQuote::ticker).containsExactly("BBAR", "BMA");
    }

    @Test
    void attachesLatestPriceAndDailyReturn() {
        stubSearch("pamp", Account10001Fixture.PAMP);
        when(prices.findLatestByInstrumentIds(List.of(47L)))
                .thenReturn(Map.of(47L, Account10001Fixture.latestPrices().get(Account10001Fixture.PAMP.id())));

        InstrumentQuote quote = service.search("pamp", PAGE).content().getFirst();

        assertThat(quote.ticker()).isEqualTo("PAMP");
        assertThat(quote.lastPrice()).isEqualByComparingTo("925.85");
        assertThat(quote.dailyReturnPercent()).isEqualByComparingTo("0.44");
    }

    @Test
    void missingCloseLeavesPriceUnavailable() {
        Instrument instrument = new Instrument(99L, "XXXX", "No price", InstrumentType.ACCIONES);
        stubSearch("xxxx", instrument);
        when(prices.findLatestByInstrumentIds(List.of(99L))).thenReturn(Map.of());

        InstrumentQuote quote = service.search("xxxx", PAGE).content().getFirst();

        assertThat(quote.lastPrice()).isNull();
        assertThat(quote.dailyReturnPercent()).isNull();
    }

    @Test
    void zeroPreviousCloseLeavesDailyReturnUnavailable() {
        stubSearch("pamp", Account10001Fixture.PAMP);
        when(prices.findLatestByInstrumentIds(List.of(47L))).thenReturn(Map.of(47L,
                new MarketPrice(1L, 47L, null, null, null, new BigDecimal("10.00"), BigDecimal.ZERO, LocalDate.now())));

        assertThat(service.search("pamp", PAGE).content().getFirst().dailyReturnPercent()).isNull();
    }

    @Test
    void blankQueryIsRejected() {
        assertThatThrownBy(() -> service.search("   ", PAGE))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_EMPTY.message());
        assertThatThrownBy(() -> service.search("", PAGE))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_EMPTY.message());
        verifyNoInteractions(instruments);
    }

    @Test
    void queryAboveOneHundredCharactersIsRejected() {
        assertThatThrownBy(() -> service.search("n".repeat(101), PAGE))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_TOO_LONG.message());
    }

    @Test
    void hundredCharactersAreAccepted() {
        when(instruments.search(anyString(), any(), any())).thenReturn(page());

        assertThat(service.search("n".repeat(100), PAGE).content()).isEmpty();
    }

    @Test
    void controlCharacterIsRejected() {
        assertThatThrownBy(() -> service.search("pamp\n", PAGE))
                .isInstanceOf(DomainException.class)
                .hasMessage(DomainErrorCodes.SEARCH_INVALID.message());
    }

    @Test
    void nullCharacterIsRejected() {
        assertThatThrownBy(() -> service.search("pamp\u0000", PAGE))
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
        when(instruments.search(text, InstrumentType.MONEDA, PAGE)).thenReturn(page(instrument));
    }

    private static PageResult<Instrument> page(Instrument... found) {
        return new PageResult<>(List.of(found), PAGE.page(), PAGE.size(), found.length);
    }
}
