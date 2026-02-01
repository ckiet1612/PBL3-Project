package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    List<Brand> findAllByIsDeletedFalse();
}
