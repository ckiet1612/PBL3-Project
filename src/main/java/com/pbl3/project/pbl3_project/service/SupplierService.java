package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Supplier;
import com.pbl3.project.pbl3_project.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SupplierService {
    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAllByIsDeletedFalse();
    }

    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public void deleteSupplier(Long id) {
        supplierRepository.findById(id).ifPresent(supplier -> {
            supplier.setDeleted(true);
            supplierRepository.save(supplier);
        });
    }
}
