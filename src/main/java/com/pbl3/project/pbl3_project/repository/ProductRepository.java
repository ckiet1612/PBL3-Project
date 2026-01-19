package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
