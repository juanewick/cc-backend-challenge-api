package com.cc.brokerage.domain.port.in;

import com.cc.brokerage.domain.model.ListedOrder;

import java.util.List;

public interface ListOrdersUseCase {

    List<ListedOrder> list(String accountNumber);
}
