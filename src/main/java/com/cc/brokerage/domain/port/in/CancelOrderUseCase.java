package com.cc.brokerage.domain.port.in;

import com.cc.brokerage.domain.model.ListedOrder;

public interface CancelOrderUseCase {

    ListedOrder cancel(String accountNumber, long orderId);
}
