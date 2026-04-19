package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.dto.CustomerOrderAggregate;
import com.pbl3.project.pbl3_project.entity.Customer;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.CustomerRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final AuthorizationService authorizationService;

    public CustomerService(
        CustomerRepository customerRepository,
        OrderRepository orderRepository,
        AuthorizationService authorizationService
    ) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public Page<Customer> searchCustomers(User actor, String search, Boolean enabled, Pageable pageable) {
        authorizationService.requireCustomersAccess(actor);
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.ASC, "fullName"),
            Set.of("id", "fullName", "phone", "enabled", "createdAt", "updatedAt")
        );
        return customerRepository.findAll(buildCustomerSpec(search, enabled), sanitizedPageable);
    }

    @Transactional(readOnly = true)
    public Page<Customer> searchActiveCustomersForSales(User actor, String search, Pageable pageable) {
        authorizationService.requireSalesAccess(actor);
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.ASC, "fullName"),
            Set.of("id", "fullName", "phone", "createdAt")
        );
        return customerRepository.findAll(buildCustomerSpec(search, Boolean.TRUE), sanitizedPageable);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(User actor, Long customerId) {
        authorizationService.requireCustomersAccess(actor);
        return customerRepository.findById(customerId)
            .orElseThrow(() -> new ValidationException("Customer not found"));
    }

    @Transactional
    public Customer createCustomer(User actor, String fullName, String phone) {
        authorizationService.requireCustomersAccess(actor);
        String normalizedName = normalizeName(fullName);
        String normalizedPhone = normalizePhone(phone);
        ensureUniquePhone(normalizedPhone, null);

        Customer customer = new Customer();
        customer.setFullName(normalizedName);
        customer.setPhone(normalizedPhone);
        customer.setEnabled(true);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(User actor, Long customerId, String fullName, String phone) {
        authorizationService.requireCustomersAccess(actor);
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ValidationException("Customer not found"));

        String normalizedName = normalizeName(fullName);
        String normalizedPhone = normalizePhone(phone);
        ensureUniquePhone(normalizedPhone, customerId);

        customer.setFullName(normalizedName);
        customer.setPhone(normalizedPhone);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer setCustomerEnabled(User actor, Long customerId, boolean enabled) {
        authorizationService.requireCustomersAccess(actor);
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ValidationException("Customer not found"));
        customer.setEnabled(enabled);
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public CustomerOrderAggregate getCustomerAggregate(User actor, Long customerId) {
        authorizationService.requireCustomersAccess(actor);
        return getCustomerAggregates(actor, List.of(getCustomerById(actor, customerId))).getOrDefault(
            customerId,
            CustomerOrderAggregate.empty(customerId)
        );
    }

    @Transactional(readOnly = true)
    public Map<Long, CustomerOrderAggregate> getCustomerAggregates(User actor, Collection<Customer> customers) {
        authorizationService.requireCustomersAccess(actor);
        List<Long> customerIds = customers == null
            ? List.of()
            : customers.stream()
                .filter(Objects::nonNull)
                .map(Customer::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (customerIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, CustomerOrderAggregate> aggregates = new LinkedHashMap<>();
        for (CustomerOrderAggregate aggregate : orderRepository.findCustomerOrderAggregates(customerIds)) {
            aggregates.put(aggregate.customerId(), aggregate);
        }
        for (Long customerId : customerIds) {
            aggregates.putIfAbsent(customerId, CustomerOrderAggregate.empty(customerId));
        }
        return aggregates;
    }

    @Transactional(readOnly = true)
    public Page<Order> searchCustomerOrders(User actor, Long customerId, Pageable pageable) {
        authorizationService.requireCustomersAccess(actor);
        customerRepository.findById(customerId)
            .orElseThrow(() -> new ValidationException("Customer not found"));

        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.DESC, "createdAt"),
            Set.of("id", "createdAt", "totalPrice", "status")
        );

        Specification<Order> spec = (root, query, cb) -> cb.equal(root.get("customer").get("id"), customerId);
        return orderRepository.findAll(spec, sanitizedPageable);
    }

    @Transactional(readOnly = true)
    public Customer getActiveCustomerForSales(User actor, Long customerId) {
        authorizationService.requireSalesAccess(actor);
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ValidationException("Customer not found"));
        if (!customer.isEnabled()) {
            throw new ValidationException("Selected customer is disabled");
        }
        return customer;
    }

    private Specification<Customer> buildCustomerSpec(String search, Boolean enabled) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), likeValue)
                ));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void ensureUniquePhone(String phone, Long ignoreCustomerId) {
        customerRepository.findByPhoneIgnoreCase(phone)
            .filter(existing -> ignoreCustomerId == null || !existing.getId().equals(ignoreCustomerId))
            .ifPresent(existing -> {
                throw new ValidationException("Phone already exists");
            });
    }

    private String normalizeName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Customer name is required");
        }
        return fullName.trim().replaceAll("\\s+", " ");
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new ValidationException("Phone is required");
        }
        String normalized = phone.trim().replaceAll("\\s+", "");
        if (normalized.isEmpty()) {
            throw new ValidationException("Phone is required");
        }
        return normalized;
    }
}
