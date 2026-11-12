package com.cc.brokerage.domain.port.in;

import com.cc.brokerage.domain.model.Order;

public interface CancelOrderUseCase {

    Order cancel(String accountNumber, Long orderId);
}
