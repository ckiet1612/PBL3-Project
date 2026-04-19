package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.StocktakeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface StocktakeSessionRepository extends JpaRepository<StocktakeSession, Long>, JpaSpecificationExecutor<StocktakeSession> {

    @org.springframework.data.jpa.repository.Query("""
        SELECT s
        FROM StocktakeSession s
        LEFT JOIN FETCH s.items
        WHERE s.id = :id
    """)
    java.util.Optional<StocktakeSession> findByIdWithItems(@org.springframework.data.repository.query.Param("id") Long id);
}
