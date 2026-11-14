package com.cc.brokerage.adapter.out.persistence;

import com.cc.brokerage.domain.model.PageQuery;
import com.cc.brokerage.domain.model.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.function.Function;

/**
 * Converts between the domain page types and Spring Data's.
 */
final class PageMapping {

    private PageMapping() {
    }

    static PageRequest toRequest(PageQuery page) {
        return PageRequest.of(page.page(), page.size());
    }

    static <E, D> PageResult<D> toResult(Page<E> found, PageQuery page, Function<? super E, ? extends D> mapper) {
        return new PageResult<>(
                found.getContent().stream().<D>map(mapper).toList(),
                page.page(),
                page.size(),
                found.getTotalElements()
        );
    }
}
