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
import com.cc.brokerage.domain.service.LatestPrices;
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

    public PlaceOrderService(
            UserRepository users,
            InstrumentRepository instruments,
            MarketPriceRepository prices,
            OrderRepository orders,
            PortfolioCalculator calculator
    ) {
        this.users = users;
        this.instruments = instruments;
        this.prices = prices;
        this.orders = orders;
        this.calculator = calculator;
    }

    @Override
    @Transactional
    public PlaceOrderOutcome place(PlaceOrderCommand command) {
        String accountNumber = InputText.account(command.accountNumber());
        String ticker = InputText.ticker(command.ticker());
        validateShape(command);
        User user = users.lockByAccountNumber(accountNumber);
        Instrument instrument = instruments.findByTicker(ticker)
                .orElseThrow(() -> new DomainException(DomainErrorCodes.INSTRUMENT_NOT_FOUND));
        if (instrument.type() == InstrumentType.MONEDA
                || command.side() == OrderSide.CASH_IN
                || command.side() == OrderSide.CASH_OUT) {
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
                MoneyMath.scaled(price),
                command.type(),
                command.side(),
                decision.status(),
                LocalDateTime.now()
        );
        BigDecimal amount = price == null ? null : MoneyMath.notional(size, price);
        return new PlaceOrderOutcome(orders.save(order), amount, decision.reason());
    }

    private void validateShape(PlaceOrderCommand command) {
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
        if (hasSize && (command.size() < MoneyMath.minShares() || command.size() > MoneyMath.maxShares())) {
            throw new DomainException(DomainErrorCodes.INVALID_SIZE, MoneyMath.minShares(), MoneyMath.maxShares());
        }
        if (hasAmount) {
            acceptAmount(command.amount());
        }
        if (command.type() == OrderType.LIMIT) {
            if (command.price() == null) {
                throw new DomainException(DomainErrorCodes.PRICE_NOT_POSITIVE);
            }
            acceptPrice(command.price());
        }
    }

    private static void acceptAmount(BigDecimal value) {
        acceptMoney(value, DomainErrorCodes.AMOUNT_NOT_POSITIVE, DomainErrorCodes.AMOUNT_ABOVE_MAXIMUM);
    }

    private static void acceptPrice(BigDecimal value) {
        acceptMoney(value, DomainErrorCodes.PRICE_NOT_POSITIVE, DomainErrorCodes.PRICE_ABOVE_MAXIMUM);
    }

    private static void acceptMoney(BigDecimal value, DomainErrorCodes notPositive, DomainErrorCodes aboveMaximum) {
        BigDecimal rounded = rounded(value);
        if (rounded.compareTo(MoneyMath.minAmount()) <= 0) {
            throw new DomainException(notPositive);
        }
        if (rounded.compareTo(MoneyMath.maxAmount()) > 0) {
            throw new DomainException(aboveMaximum);
        }
    }

    private static BigDecimal rounded(BigDecimal value) {
        return value.setScale(MoneyMath.scale(), MoneyMath.ROUNDING);
    }

    private BigDecimal resolvePrice(PlaceOrderCommand command, Instrument instrument) {
        if (command.type() == OrderType.LIMIT) {
            return rounded(command.price());
        }
        return LatestPrices.one(prices.findByInstrumentIds(Set.of(instrument.id())))
                .map(MarketPrice::close)
                .orElse(null);
    }

    private int resolveSize(PlaceOrderCommand command, BigDecimal price) {
        if (command.size() != null) {
            return command.size();
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return MoneyMath.sharesFromAmount(rounded(command.amount()), price);
    }

    private Decision decideStatus(PlaceOrderCommand command, User user, Instrument instrument, int size, BigDecimal price) {
        if (command.type() == OrderType.MARKET && price == null) {
            return new Decision(OrderStatus.REJECTED, RejectionReason.NO_MARKET_PRICE);
        }
        if (size < 1) {
            return new Decision(OrderStatus.REJECTED, RejectionReason.ZERO_SHARES);
        }
        var holdings = calculator.summarize(orders.findByUserId(user.id()));
        BigDecimal cost = MoneyMath.notional(size, price);
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
