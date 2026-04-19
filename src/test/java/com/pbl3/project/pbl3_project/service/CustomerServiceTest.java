package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Customer;
import com.pbl3.project.pbl3_project.repository.CustomerRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AuthorizationService authorizationService;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, orderRepository, authorizationService);
    }

    @Test
    void createCustomerNormalizesNameAndPhoneBeforeSave() {
        when(customerRepository.findByPhoneIgnoreCase("0901234567")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer saved = customerService.createCustomer(null, "  Nguyen   Van   A  ", " 0901 234 567 ");

        assertEquals("Nguyen Van A", saved.getFullName());
        assertEquals("0901234567", saved.getPhone());
        assertTrue(saved.isEnabled());
    }

    @Test
    void createCustomerRejectsDuplicatePhone() {
        Customer existing = new Customer();
        existing.setId(9L);
        existing.setPhone("0901234567");
        when(customerRepository.findByPhoneIgnoreCase("0901234567")).thenReturn(Optional.of(existing));

        ValidationException ex = assertThrows(
            ValidationException.class,
            () -> customerService.createCustomer(null, "Alice", "0901234567")
        );

        assertEquals("Phone already exists", ex.getMessage());
    }
}
