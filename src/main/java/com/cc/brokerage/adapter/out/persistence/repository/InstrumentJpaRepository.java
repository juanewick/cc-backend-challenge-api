package com.cc.brokerage.adapter.out.persistence.repository;

import com.cc.brokerage.adapter.out.persistence.entity.InstrumentEntity;
import com.cc.brokerage.domain.model.InstrumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InstrumentJpaRepository extends JpaRepository<InstrumentEntity, Long> {

    Optional<InstrumentEntity> findByTickerIgnoreCase(String ticker);

    /**
     * {@code pattern} is already lower case and escaped with {@code !}.
     */
    @Query(value = """
            select i from InstrumentEntity i
            where i.type <> :excluded
              and (lower(i.ticker) like :pattern escape '!' or lower(i.name) like :pattern escape '!')
            order by i.ticker, i.id
            """,
            countQuery = """
            select count(i) from InstrumentEntity i
            where i.type <> :excluded
              and (lower(i.ticker) like :pattern escape '!' or lower(i.name) like :pattern escape '!')
            """)
    Page<InstrumentEntity> search(@Param("pattern") String pattern, @Param("excluded") InstrumentType excluded, Pageable pageable);
}
