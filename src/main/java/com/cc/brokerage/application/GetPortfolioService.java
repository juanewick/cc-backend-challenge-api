package com.cc.brokerage.application;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentHoldings;
import com.cc.brokerage.domain.model.Portfolio;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.in.GetPortfolioUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import com.cc.brokerage.domain.service.PortfolioCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class GetPortfolioService implements GetPortfolioUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetPortfolioService.class);

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
        Map<Long, Instrument> found = instruments.findByIds(held);
        if (found.size() < held.size()) {
            // The calculator skips a position whose instrument is gone, so this is the only trace of it.
            log.warn("Portfolio of user {} leaves out positions on missing instruments {}",
                    user.id(), held.stream().filter(id -> !found.containsKey(id)).toList());
        }
        Portfolio portfolio = calculator.toPortfolio(user, holdings, found, prices.findLatestByInstrumentIds(held));
        if (!portfolio.unpricedTickers().isEmpty()) {
            log.warn("Portfolio of user {} has positions without market price, left out of the total: {}",
                    user.id(), portfolio.unpricedTickers());
        }
        log.debug("Portfolio of user {} read: {} positions", user.id(), portfolio.positions().size());
        return portfolio;
    }
}
