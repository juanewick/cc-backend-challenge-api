package com.cc.brokerage.application;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.InstrumentHoldings;
import com.cc.brokerage.domain.model.Portfolio;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.in.GetPortfolioUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import com.cc.brokerage.domain.service.PortfolioCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class GetPortfolioService implements GetPortfolioUseCase {

    private final UserRepository users;
    private final OrderRepository orders;
    private final InstrumentRepository instruments;
    private final MarketPriceRepository prices;
    private final PortfolioCalculator calculator;

    public GetPortfolioService(
            UserRepository users,
            OrderRepository orders,
            InstrumentRepository instruments,
            MarketPriceRepository prices,
            PortfolioCalculator calculator
    ) {
        this.users = users;
        this.orders = orders;
        this.instruments = instruments;
        this.prices = prices;
        this.calculator = calculator;
    }

    @Override
    @Transactional(readOnly = true)
    public Portfolio getPortfolio(String accountNumber) {
        String account = InputText.account(accountNumber);
        User user = users.findByAccountNumber(account)
                .orElseThrow(() -> new DomainException(DomainErrorCodes.ACCOUNT_NOT_FOUND));
        List<InstrumentHoldings> holdings = orders.summarizeHoldings(user.id());
        List<Long> held = holdings.stream()
                .filter(row -> row.netQuantity() != 0)
                .map(InstrumentHoldings::instrumentId)
                .filter(Objects::nonNull)
                .toList();
        return calculator.toPortfolio(user, holdings, instruments.findByIds(held), prices.findLatestByInstrumentIds(held));
    }
}
