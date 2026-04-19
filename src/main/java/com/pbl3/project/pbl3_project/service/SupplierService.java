package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Supplier;
import com.pbl3.project.pbl3_project.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SupplierService {
    private final SupplierRepository supplierRepository;
    private final AuthorizationService authorizationService;

    public SupplierService(SupplierRepository supplierRepository, AuthorizationService authorizationService) {
        this.supplierRepository = supplierRepository;
        this.authorizationService = authorizationService;
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAllByIsDeletedFalse();
    }

    public Page<Supplier> searchSuppliers(String search, Pageable pageable) {
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.ASC, "name"),
            java.util.Set.of("name", "phone")
        );
        return supplierRepository.findAll(
            (root, query, cb) -> {
                var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                predicates.add(cb.isFalse(root.get("isDeleted")));
                if (search != null && !search.trim().isEmpty()) {
                    String likeValue = "%" + search.trim().toLowerCase() + "%";
                    predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("name"), "")), likeValue),
                        cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), likeValue),
                        cb.like(cb.lower(cb.coalesce(root.get("address"), "")), likeValue)
                    ));
                }
                return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
            },
            sanitizedPageable
        );
    }

    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public Supplier saveSupplier(Supplier supplier, com.pbl3.project.pbl3_project.entity.User actor) {
        authorizationService.requireMasterDataAccess(actor);
        return saveSupplier(supplier);
    }

    public void deleteSupplier(Long id) {
        supplierRepository.findById(id).ifPresent(supplier -> {
            supplier.setDeleted(true);
            supplierRepository.save(supplier);
        });
    }

    public void deleteSupplier(Long id, com.pbl3.project.pbl3_project.entity.User actor) {
        authorizationService.requireMasterDataAccess(actor);
        deleteSupplier(id);
    }
}
