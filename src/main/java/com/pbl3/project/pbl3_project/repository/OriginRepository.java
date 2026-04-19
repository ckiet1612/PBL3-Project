package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.Origin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OriginRepository extends JpaRepository<Origin, Long>, JpaSpecificationExecutor<Origin> {
    List<Origin> findAllByIsDeletedFalse();
}
