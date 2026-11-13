package com.cc.brokerage.UnitTest;

import com.cc.brokerage.application.SearchInstrumentsService;
import com.cc.brokerage.domain.InvalidRequestException;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentQuote;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.support.Account10001Fixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchInstrumentsServiceTest {

    @Mock
    private InstrumentRepository instruments;
    @Mock
    private MarketPriceRepository prices;
    @InjectMocks
    private SearchInstrumentsService service;

    @Test
    void matchesTickerOrNameWithoutDuplicatesAndSorted() {
        when(instruments.findAll()).thenReturn(List.of(
                new Instrument(22L, "BBAR", "Banco Frances", InstrumentType.ACCIONES),
                new Instrument(28L, "BHIP", "Banco Hipotecario S.A.", InstrumentType.ACCIONES),
                Account10001Fixture.BMA,
                new Instrument(19L, "BPAT", "Banco Patagonia", InstrumentType.ACCIONES),
                Account10001Fixture.PAMP
        ));
        when(prices.findByInstrumentIds(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        List<InstrumentQuote> result = service.search("banco");

        assertThat(result).extracting(InstrumentQuote::ticker).containsExactly("BBAR", "BHIP", "BMA", "BPAT");
    }

    @Test
    void attachesLatestPriceAndDailyReturn() {
        when(instruments.findAll()).thenReturn(List.of(Account10001Fixture.PAMP));
        when(prices.findByInstrumentIds(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(Account10001Fixture.latestPrices().get(Account10001Fixture.PAMP.id())));

        InstrumentQuote quote = service.search("pamp").getFirst();

        assertThat(quote.ticker()).isEqualTo("PAMP");
        assertThat(quote.lastPrice()).isEqualByComparingTo("925.85");
        assertThat(quote.dailyReturnPercent()).isEqualByComparingTo("0.44");
    }

    @Test
    void missingCloseLeavesPriceUnavailable() {
        Instrument instrument = new Instrument(99L, "XXXX", "Sin precio", InstrumentType.ACCIONES);
        when(instruments.findAll()).thenReturn(List.of(instrument));
        when(prices.findByInstrumentIds(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        InstrumentQuote quote = service.search("xxxx").getFirst();

        assertThat(quote.lastPrice()).isNull();
        assertThat(quote.dailyReturnPercent()).isNull();
    }

    @Test
    void zeroPreviousCloseLeavesDailyReturnUnavailable() {
        Instrument instrument = Account10001Fixture.PAMP;
        when(instruments.findAll()).thenReturn(List.of(instrument));
        when(prices.findByInstrumentIds(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
                new MarketPrice(1L, 47L, null, null, null, new BigDecimal("10.00"), BigDecimal.ZERO, LocalDate.now())
        ));

        assertThat(service.search("pamp").getFirst().dailyReturnPercent()).isNull();
    }

    @Test
    void blankQueryIsRejected() {
        assertThatThrownBy(() -> service.search("   "))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("La búsqueda no tiene texto");
        assertThatThrownBy(() -> service.search(""))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("La búsqueda no tiene texto");
    }

    @Test
    void queryAboveOneHundredCharactersIsRejected() {
        assertThatThrownBy(() -> service.search("n".repeat(101)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("La búsqueda supera el máximo permitido");
    }

    @Test
    void hundredCharactersAreAccepted() {
        when(instruments.findAll()).thenReturn(List.of());

        assertThat(service.search("n".repeat(100))).isEmpty();
    }

    @Test
    void controlCharacterIsRejected() {
        assertThatThrownBy(() -> service.search("pamp\n"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("La búsqueda no es válida");
    }

    @Test
    void nullCharacterIsRejected() {
        assertThatThrownBy(() -> service.search("pamp\u0000"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("La búsqueda no es válida");
    }

    @Test
    void upperAndLowerCaseFindTheSameInstrument() {
        when(instruments.findAll()).thenReturn(List.of(Account10001Fixture.PAMP));
        when(prices.findByInstrumentIds(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        assertThat(service.search("PAMP")).extracting(InstrumentQuote::ticker).containsExactly("PAMP");
        assertThat(service.search("pamp")).extracting(InstrumentQuote::ticker).containsExactly("PAMP");
        assertThat(service.search("  pamp  ")).extracting(InstrumentQuote::ticker).containsExactly("PAMP");
    }

    @Test
    void injectionShapedQueryDoesNotAddRows() {
        when(instruments.findAll()).thenReturn(List.of(Account10001Fixture.PAMP, Account10001Fixture.BMA));

        assertThat(service.search("pamp' OR 1=1")).isEmpty();
    }

    @Test
    void matchDoesNotDependOnMachineLocale() {
        Locale previous = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        try {
            when(instruments.findAll()).thenReturn(List.of(
                    new Instrument(8L, "TIP", "Titulo", InstrumentType.ACCIONES)));
            when(prices.findByInstrumentIds(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

            assertThat(service.search("tip")).extracting(InstrumentQuote::ticker).containsExactly("TIP");
        } finally {
            Locale.setDefault(previous);
        }
    }
}
