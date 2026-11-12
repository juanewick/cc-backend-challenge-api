package com.cc.brokerage.domain.port.in;

import com.cc.brokerage.domain.model.Order;
import com.cc.brokerage.domain.model.PlaceOrderCommand;

public interface PlaceOrderUseCase {

    Order place(PlaceOrderCommand command);
}
