package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByPhoneIgnoreCase(String phone);
}
