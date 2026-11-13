package com.cc.brokerage.adapter.out.persistence.repository;

import com.cc.brokerage.adapter.out.persistence.entity.OrderEntity;
import com.cc.brokerage.domain.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    @Query(value = """
            select o from OrderEntity o
            where o.userId = :userId
            order by o.datetime asc nulls last, o.id asc
            """,
            countQuery = "select count(o) from OrderEntity o where o.userId = :userId")
    Page<OrderEntity> findPageByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = """
            select o from OrderEntity o
            where o.userId = :userId and o.status = :status
            order by o.datetime asc nulls last, o.id asc
            """,
            countQuery = "select count(o) from OrderEntity o where o.userId = :userId and o.status = :status")
    Page<OrderEntity> findPageByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status, Pageable pageable);

    /**
     * Adds up the FILLED and NEW orders of a user per instrument, in the database, so the service never loads the
     * order history. Status and side are compared as stored text; anything else is ignored. size * price is
     * exact because price is NUMERIC(10,2), and SUM over INT returns BIGINT, so quantities cannot overflow here.
     */
    @Query(value = """
            SELECT CAST(o.instrumentid AS BIGINT) AS "instrumentId",
                   COALESCE(SUM(CASE WHEN o.status = 'FILLED' AND o.side = 'BUY' THEN o.size
                                     WHEN o.status = 'FILLED' AND o.side = 'SELL' THEN -o.size END), 0) AS "netQuantity",
                   COALESCE(SUM(CASE WHEN o.status = 'FILLED' AND o.side = 'BUY' THEN o.size * o.price
                                     WHEN o.status = 'FILLED' AND o.side = 'SELL' THEN -(o.size * o.price) END), 0) AS "netInvested",
                   COALESCE(SUM(CASE WHEN o.status = 'NEW' AND o.side = 'SELL' THEN o.size END), 0) AS "reservedSellQuantity",
                   COALESCE(SUM(CASE WHEN o.status = 'NEW' AND o.side = 'BUY' THEN o.size * o.price END), 0) AS "reservedBuyAmount",
                   COALESCE(SUM(CASE WHEN o.status = 'FILLED' AND o.side = 'CASH_IN' THEN o.size * o.price
                                     WHEN o.status = 'FILLED' AND o.side = 'CASH_OUT' THEN -(o.size * o.price) END), 0) AS "cashFlow",
                   COUNT(*) FILTER (WHERE o.price IS NULL) AS "unpricedOrders"
            FROM orders o
            WHERE o.userid = :userId AND o.status IN ('FILLED', 'NEW')
            GROUP BY o.instrumentid
            """, nativeQuery = true)
    List<HoldingsRow> summarizeHoldings(@Param("userId") Long userId);

    interface HoldingsRow {
        Long getInstrumentId();

        Long getNetQuantity();

        BigDecimal getNetInvested();

        Long getReservedSellQuantity();

        BigDecimal getReservedBuyAmount();

        BigDecimal getCashFlow();

        Long getUnpricedOrders();
    }
}
