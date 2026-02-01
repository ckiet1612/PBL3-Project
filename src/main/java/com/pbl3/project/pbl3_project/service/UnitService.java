package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Unit;
import com.pbl3.project.pbl3_project.repository.UnitRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UnitService {
    private final UnitRepository unitRepository;

    public UnitService(UnitRepository unitRepository) {
        this.unitRepository = unitRepository;
    }

    public List<Unit> getAllUnits() {
        return unitRepository.findAllByIsDeletedFalse();
    }

    public Unit saveUnit(Unit unit) {
        return unitRepository.save(unit);
    }

    public void deleteUnit(Long id) {
        unitRepository.findById(id).ifPresent(unit -> {
            unit.setDeleted(true);
            unitRepository.save(unit);
        });
    }
}
