package com.cc.brokerage.application;

import com.cc.brokerage.domain.InstrumentNotFoundException;
import com.cc.brokerage.domain.InvalidRequestException;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class PlaceOrderService implements PlaceOrderUseCase {

    private final UserRepository users;
    private final InstrumentRepository instruments;
    private final MarketPriceRepository prices;
    private final OrderRepository orders;
    private final PortfolioCalculator calculator;
    private final Clock clock;

    public PlaceOrderService(
            UserRepository users,
            InstrumentRepository instruments,
            MarketPriceRepository prices,
            OrderRepository orders,
            PortfolioCalculator calculator,
            Clock clock
    ) {
        this.users = users;
        this.instruments = instruments;
        this.prices = prices;
        this.orders = orders;
        this.calculator = calculator;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PlaceOrderOutcome place(PlaceOrderCommand command) {
        String accountNumber = InputText.account(command.accountNumber());
        String ticker = InputText.ticker(command.ticker());
        validateShape(command);
        User user = users.lockByAccountNumber(accountNumber);
        Instrument instrument = instruments.findByTicker(ticker)
                .orElseThrow(InstrumentNotFoundException::new);
        if (instrument.type() == InstrumentType.MONEDA
                || command.side() == OrderSide.CASH_IN
                || command.side() == OrderSide.CASH_OUT) {
            throw new InvalidRequestException("No se registran ingresos ni egresos de pesos nuevos");
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
                LocalDateTime.now(clock)
        );
        BigDecimal amount = price == null ? null : MoneyMath.notional(size, price);
        return new PlaceOrderOutcome(orders.save(order), amount, decision.reason());
    }

    private void validateShape(PlaceOrderCommand command) {
        if (command.side() != OrderSide.BUY && command.side() != OrderSide.SELL) {
            throw new InvalidRequestException("El lado debe ser BUY o SELL");
        }
        if (command.type() == null) {
            throw new InvalidRequestException("El tipo de orden es obligatorio");
        }
        boolean hasSize = command.size() != null;
        boolean hasAmount = command.amount() != null;
        if (hasSize == hasAmount) {
            throw new InvalidRequestException("Debe indicarse exactamente una forma de cantidad: acciones o monto");
        }
        if (hasSize && (command.size() < MoneyMath.minShares() || command.size() > MoneyMath.maxShares())) {
            throw new InvalidRequestException(MoneyMath.sharesRangeMessage());
        }
        if (hasAmount) {
            acceptMoney(command.amount(), "El monto debe ser mayor que cero", "El monto supera el máximo permitido");
        }
        if (command.type() == OrderType.LIMIT) {
            if (command.price() == null) {
                throw new InvalidRequestException("El precio es obligatorio y debe ser mayor que cero");
            }
            acceptMoney(command.price(), "El precio es obligatorio y debe ser mayor que cero", "El precio supera el máximo permitido");
        }
    }

    private static void acceptMoney(BigDecimal value, String notPositive, String aboveMaximum) {
        BigDecimal rounded = rounded(value);
        if (rounded.compareTo(MoneyMath.minAmount()) <= 0) {
            throw new InvalidRequestException(notPositive);
        }
        if (rounded.compareTo(MoneyMath.maxAmount()) > 0) {
            throw new InvalidRequestException(aboveMaximum);
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
