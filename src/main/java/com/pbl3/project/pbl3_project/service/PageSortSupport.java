package com.pbl3.project.pbl3_project.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Collection;

final class PageSortSupport {

    private PageSortSupport() {
    }

    static Pageable sanitize(Pageable pageable, Sort defaultSort, Collection<String> allowedProperties) {
        int page = pageable != null ? pageable.getPageNumber() : 0;
        int size = pageable != null ? pageable.getPageSize() : 20;
        Sort requestedSort = pageable != null ? pageable.getSort() : Sort.unsorted();

        java.util.List<Sort.Order> allowedOrders = new ArrayList<>();
        if (requestedSort != null) {
            for (Sort.Order order : requestedSort) {
                if (allowedProperties.contains(order.getProperty())) {
                    allowedOrders.add(order);
                }
            }
        }

        Sort sanitizedSort = allowedOrders.isEmpty() ? defaultSort : Sort.by(allowedOrders);
        return PageRequest.of(page, size, sanitizedSort);
    }
}
