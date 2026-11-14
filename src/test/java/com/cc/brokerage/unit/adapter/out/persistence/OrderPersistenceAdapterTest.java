package com.cc.brokerage.unit.adapter.out.persistence;

import com.cc.brokerage.adapter.out.persistence.OrderPersistenceAdapter;
import com.cc.brokerage.adapter.out.persistence.repository.OrderJpaRepository;
import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.support.Account10001Fixture;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class OrderPersistenceAdapterTest {

    @Test
    void orderWithAPlaceholderValueIsNeverStored() {
        OrderJpaRepository orders = mock(OrderJpaRepository.class);
        OrderPersistenceAdapter adapter = new OrderPersistenceAdapter(orders);

        assertThatThrownBy(() -> adapter.save(Account10001Fixture.order(
                5L, 45L, 50, "710", OrderSide.UNKNOWN, OrderStatus.CANCELLED, OrderType.LIMIT, "2023-07-12T15:14:20")))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(orders);
    }
}
