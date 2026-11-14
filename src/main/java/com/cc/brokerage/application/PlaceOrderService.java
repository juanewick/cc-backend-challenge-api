package com.cc.brokerage.application;

import com.cc.brokerage.domain.DomainErrorCodes;
import com.cc.brokerage.domain.DomainException;
import com.cc.brokerage.domain.model.InputText;
import com.cc.brokerage.domain.model.Instrument;
import com.cc.brokerage.domain.model.InstrumentType;
import com.cc.brokerage.domain.model.MarketPrice;
import com.cc.brokerage.domain.model.MoneyMath;
import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.domain.model.PlaceOrderCommand;
import com.cc.brokerage.domain.model.PlaceOrderOutcome;
import com.cc.brokerage.domain.model.RejectionReason;
import com.cc.brokerage.domain.model.User;
import com.cc.brokerage.domain.port.in.PlaceOrderUseCase;
import com.cc.brokerage.domain.port.out.InstrumentRepository;
import com.cc.brokerage.domain.port.out.MarketPriceRepository;
import com.cc.brokerage.domain.port.out.OrderRepository;
import com.cc.brokerage.domain.port.out.UserRepository;
import com.cc.brokerage.domain.service.PortfolioCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class PlaceOrderService implements PlaceOrderUseCase {

    private final UserRepository users;
    private final InstrumentRepository instruments;
    private final MarketPriceRepository prices;
    private final OrderRepository orders;
    private final PortfolioCalculator calculator;
    private final MoneyMath money;

    public PlaceOrderService(
            UserRepository users,
            InstrumentRepository instruments,
            MarketPriceRepository prices,
            OrderRepository orders,
            PortfolioCalculator calculator,
            MoneyMath money
    ) {
        this.users = users;
        this.instruments = instruments;
        this.prices = prices;
        this.orders = orders;
        this.calculator = calculator;
        this.money = money;
    }

    @Override
    @Transactional
    public PlaceOrderOutcome place(PlaceOrderCommand command) {
        String accountNumber = InputText.account(command.accountNumber());
        String ticker = InputText.ticker(command.ticker());
        validate(command);
        User user = users.lockByAccountNumber(accountNumber);
        Instrument instrument = instruments.findByTicker(ticker)
                .orElseThrow(() -> new DomainException(DomainErrorCodes.INSTRUMENT_NOT_FOUND));
        if (instrument.type() == InstrumentType.MONEDA) {
            throw new DomainException(DomainErrorCodes.CASH_MOVEMENT);
        }

        BigDecimal price = resolvePrice(command, instrument);
        int size = resolveSize(command, price);
        Decision decision = decideStatus(command, user, instrument, size, price);
        Order order = new Order(
                null,
                instrument.id(),
                user.id(),
                size,
                money.scaled(price),
                command.type(),
                command.side(),
                decision.status(),
                LocalDateTime.now()
        );
        BigDecimal amount = price == null ? null : money.notional(size, price);
        return new PlaceOrderOutcome(orders.save(order), instrument.ticker(), amount, decision.reason());
    }

    /**
     * Every rule about the shape of an order request lives here, so the HTTP adapter only has to read JSON.
     */
    private void validate(PlaceOrderCommand command) {
        if (command.side() != OrderSide.BUY && command.side() != OrderSide.SELL) {
            throw new DomainException(DomainErrorCodes.INVALID_SIDE);
        }
        if (command.type() != OrderType.MARKET && command.type() != OrderType.LIMIT) {
            throw new DomainException(DomainErrorCodes.INVALID_TYPE);
        }
        boolean hasSize = command.size() != null;
        boolean hasAmount = command.amount() != null;
        if (hasSize == hasAmount) {
            throw new DomainException(DomainErrorCodes.EXCLUSIVE_QUANTITY);
        }
        if (hasSize && (command.size() < money.minShares() || command.size() > money.maxShares())) {
            throw new DomainException(DomainErrorCodes.INVALID_SIZE, money.minShares(), money.maxShares());
        }
        if (hasAmount) {
            acceptMoney(command.amount(), DomainErrorCodes.AMOUNT_NOT_POSITIVE, DomainErrorCodes.AMOUNT_ABOVE_MAXIMUM);
        }
        if (command.type() == OrderType.MARKET && command.price() != null) {
            // A client sending a price on a MARKET order most likely expects a cap; filling at the close would surprise it.
            throw new DomainException(DomainErrorCodes.PRICE_NOT_ALLOWED);
        }
        if (command.type() == OrderType.LIMIT) {
            if (command.price() == null) {
                throw new DomainException(DomainErrorCodes.PRICE_NOT_POSITIVE);
            }
            acceptMoney(command.price(), DomainErrorCodes.PRICE_NOT_POSITIVE, DomainErrorCodes.PRICE_ABOVE_MAXIMUM);
        }
    }

    private void acceptMoney(BigDecimal value, DomainErrorCodes notPositive, DomainErrorCodes aboveMaximum) {
        BigDecimal rounded = money.round(value);
        if (rounded.compareTo(money.minAmount()) <= 0) {
            throw new DomainException(notPositive);
        }
        if (rounded.compareTo(money.maxAmount()) > 0) {
            throw new DomainException(aboveMaximum);
        }
    }

    private BigDecimal resolvePrice(PlaceOrderCommand command, Instrument instrument) {
        if (command.type() == OrderType.LIMIT) {
            return money.round(command.price());
        }
        MarketPrice latest = prices.findLatestByInstrumentIds(Set.of(instrument.id())).get(instrument.id());
        return latest == null ? null : latest.close();
    }

    private int resolveSize(PlaceOrderCommand command, BigDecimal price) {
        if (command.size() != null) {
            return command.size();
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return money.sharesFromAmount(money.round(command.amount()), price);
    }

    private Decision decideStatus(PlaceOrderCommand command, User user, Instrument instrument, int size, BigDecimal price) {
        if (command.type() == OrderType.MARKET && price == null) {
            return new Decision(OrderStatus.REJECTED, RejectionReason.NO_MARKET_PRICE);
        }
        if (size < 1) {
            return new Decision(OrderStatus.REJECTED, RejectionReason.ZERO_SHARES);
        }
        var holdings = calculator.summarize(orders.summarizeHoldings(user.id()));
        BigDecimal cost = money.notional(size, price);
        if (command.side() == OrderSide.BUY && cost.compareTo(holdings.availableCash()) > 0) {
            return new Decision(OrderStatus.REJECTED, RejectionReason.INSUFFICIENT_CASH);
        }
        if (command.side() == OrderSide.SELL && size > calculator.availableShares(holdings, instrument.id())) {
            return new Decision(OrderStatus.REJECTED, RejectionReason.INSUFFICIENT_SHARES);
        }
        OrderStatus status = command.type() == OrderType.MARKET ? OrderStatus.FILLED : OrderStatus.NEW;
        return new Decision(status, null);
    }

    private record Decision(OrderStatus status, RejectionReason reason) {
    }
}
