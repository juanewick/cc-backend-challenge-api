package com.cc.brokerage.adapter.out.persistence.repository;

import com.cc.brokerage.adapter.out.persistence.entity.MarketPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MarketPriceJpaRepository extends JpaRepository<MarketPriceEntity, Long> {

    /**
     * One row per instrument: the latest date, then the highest id. DISTINCT ON is PostgreSQL.
     */
    @Query(value = """
            SELECT DISTINCT ON (m.instrumentid) m.*
            FROM marketdata m
            WHERE m.instrumentid IN (:instrumentIds)
            ORDER BY m.instrumentid, m.date DESC NULLS LAST, m.id DESC
            """, nativeQuery = true)
    List<MarketPriceEntity> findLatestByInstrumentIdIn(@Param("instrumentIds") Collection<Long> instrumentIds);
}
