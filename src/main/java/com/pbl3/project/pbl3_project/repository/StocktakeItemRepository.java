package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.StocktakeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StocktakeItemRepository extends JpaRepository<StocktakeItem, Long> {
}
