package com.cc.brokerage.domain.port.in;

import com.cc.brokerage.domain.model.ListedOrder;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.PageQuery;
import com.cc.brokerage.domain.model.PageResult;

public interface ListOrdersUseCase {

    PageResult<ListedOrder> list(String accountNumber, OrderStatus status, PageQuery page);
}
