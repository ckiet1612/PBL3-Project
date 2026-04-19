package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Unit;
import com.pbl3.project.pbl3_project.repository.UnitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UnitService {
    private final UnitRepository unitRepository;
    private final AuthorizationService authorizationService;

    public UnitService(UnitRepository unitRepository, AuthorizationService authorizationService) {
        this.unitRepository = unitRepository;
        this.authorizationService = authorizationService;
    }

    public List<Unit> getAllUnits() {
        return unitRepository.findAllByIsDeletedFalse();
    }

    public Page<Unit> searchUnits(String search, Pageable pageable) {
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.ASC, "name"),
            java.util.Set.of("name")
        );
        return unitRepository.findAll(
            (root, query, cb) -> {
                var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                predicates.add(cb.isFalse(root.get("isDeleted")));
                if (search != null && !search.trim().isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%"));
                }
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            },
            sanitizedPageable
        );
    }

    public Unit saveUnit(Unit unit) {
        return unitRepository.save(unit);
    }

    public Unit saveUnit(Unit unit, com.pbl3.project.pbl3_project.entity.User actor) {
        authorizationService.requireMasterDataAccess(actor);
        return saveUnit(unit);
    }

    public void deleteUnit(Long id) {
        unitRepository.findById(id).ifPresent(unit -> {
            unit.setDeleted(true);
            unitRepository.save(unit);
        });
    }

    public void deleteUnit(Long id, com.pbl3.project.pbl3_project.entity.User actor) {
        authorizationService.requireMasterDataAccess(actor);
        deleteUnit(id);
    }
}
