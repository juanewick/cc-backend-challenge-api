package com.cc.brokerage.adapter.out.persistence.entity;

import com.cc.brokerage.domain.model.OrderSide;
import com.cc.brokerage.domain.model.OrderStatus;
import com.cc.brokerage.domain.model.OrderType;
import com.cc.brokerage.adapter.out.persistence.converter.OrderTypeConverter;
import com.cc.brokerage.adapter.out.persistence.converter.OrderSideConverter;
import com.cc.brokerage.adapter.out.persistence.converter.OrderStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instrumentid")
    private Long instrumentId;

    @Column(name = "userid")
    private Long userId;

    @Column(name = "size")
    private Integer size;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Convert(converter = OrderTypeConverter.class)
    @Column(name = "type", length = 10)
    private OrderType type;

    @Convert(converter = OrderSideConverter.class)
    @Column(name = "side", length = 10)
    private OrderSide side;

    @Convert(converter = OrderStatusConverter.class)
    @Column(name = "status", length = 20)
    private OrderStatus status;

    @Column(name = "datetime")
    private LocalDateTime datetime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(Long instrumentId) {
        this.instrumentId = instrumentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public OrderType getType() {
        return type;
    }

    public void setType(OrderType type) {
        this.type = type;
    }

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getDatetime() {
        return datetime;
    }

    public void setDatetime(LocalDateTime datetime) {
        this.datetime = datetime;
    }
}
