package com.cc.brokerage.domain.port.in;

import com.cc.brokerage.domain.model.PlaceOrderCommand;
import com.cc.brokerage.domain.model.PlaceOrderOutcome;

public interface PlaceOrderUseCase {

    PlaceOrderOutcome place(PlaceOrderCommand command);
}
