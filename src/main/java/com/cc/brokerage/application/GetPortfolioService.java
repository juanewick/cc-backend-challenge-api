package com.cc.brokerage.application;

import com.cc.brokerage.domain.AccountNotFoundException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.model.Order;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
                .orElseThrow(AccountNotFoundException::new);
        List<Order> userOrders = orders.findByUserId(user.id());
        Set<Long> instrumentIds = userOrders.stream().map(Order::instrumentId).collect(Collectors.toSet());
        Map<Long, Instrument> instrumentMap = new HashMap<>();
        for (Long id : instrumentIds) {
            instruments.findInstrumentById(id).ifPresent(instrument -> instrumentMap.put(id, instrument));
        }
        Map<Long, MarketPrice> latest = latestPrices(instrumentIds);
        return calculator.toPortfolio(user, userOrders, instrumentMap, latest);
    }

    private Map<Long, MarketPrice> latestPrices(Set<Long> instrumentIds) {
        Map<Long, MarketPrice> latest = new HashMap<>();
        if (instrumentIds.isEmpty()) {
            return latest;
        }
        for (MarketPrice price : prices.findByInstrumentIds(instrumentIds)) {
            MarketPrice current = latest.get(price.instrumentId());
            if (current == null
                    || price.date().isAfter(current.date())
                    || (price.date().equals(current.date()) && price.id() > current.id())) {
                latest.put(price.instrumentId(), price);
            }
        }
        return latest;
    }
}
