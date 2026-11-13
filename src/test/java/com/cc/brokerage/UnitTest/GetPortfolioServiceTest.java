package com.cc.brokerage.UnitTest;

import com.cc.brokerage.application.GetPortfolioService;
import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.Portfolio;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import com.cc.brokerage.domain.service.PortfolioCalculator;
import com.cc.brokerage.support.MoneyBounds;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPortfolioServiceTest {

    @Mock
    private UserRepository users;
    @Mock
    private OrderRepository orders;
    @Mock
    private InstrumentRepository instruments;
    @Mock
    private MarketPriceRepository prices;
    @Spy
    private PortfolioCalculator calculator = new PortfolioCalculator(MoneyBounds.DEFAULT_MONEY);
    @InjectMocks
    private GetPortfolioService service;

    @Test
    void missingAccountThrows() {
        when(users.findByAccountNumber("99999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPortfolio("99999"))
                .isInstanceOf(DomainException.class)
                .extracting(ex -> ((DomainException) ex).error())
                .isEqualTo(DomainErrorCodes.ACCOUNT_NOT_FOUND);
    }

    @Test
    void accountWithoutOrdersReturnsZeros() {
        User user = new User(2L, "jose@test.com", "10002");
        when(users.findByAccountNumber("10002")).thenReturn(Optional.of(user));
        when(orders.summarizeHoldings(2L)).thenReturn(List.of());

        Portfolio portfolio = service.getPortfolio("10002");

        assertThat(portfolio.availableCash()).isEqualByComparingTo("0.00");
        assertThat(portfolio.totalValue()).isEqualByComparingTo("0.00");
        assertThat(portfolio.positions()).isEmpty();
    }
}
