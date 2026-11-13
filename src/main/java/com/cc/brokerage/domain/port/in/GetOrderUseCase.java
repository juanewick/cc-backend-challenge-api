package com.cc.brokerage.domain.port.in;

import com.cc.brokerage.domain.model.ListedOrder;

public interface GetOrderUseCase {

    ListedOrder get(String accountNumber, long orderId);
}
