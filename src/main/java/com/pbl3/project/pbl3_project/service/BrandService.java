package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Brand;
import com.pbl3.project.pbl3_project.repository.BrandRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BrandService {
    private final BrandRepository brandRepository;
    private final AuthorizationService authorizationService;

    public BrandService(BrandRepository brandRepository, AuthorizationService authorizationService) {
        this.brandRepository = brandRepository;
        this.authorizationService = authorizationService;
    }

    public List<Brand> getAllBrands() {
        return brandRepository.findAllByIsDeletedFalse();
    }

    public Page<Brand> searchBrands(String search, Pageable pageable) {
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.ASC, "name"),
            java.util.Set.of("name")
        );
        return brandRepository.findAll(
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

    public Brand saveBrand(Brand brand) {
        return brandRepository.save(brand);
    }

    public Brand saveBrand(Brand brand, com.pbl3.project.pbl3_project.entity.User actor) {
        authorizationService.requireMasterDataAccess(actor);
        return saveBrand(brand);
    }

    public void deleteBrand(Long id) {
        brandRepository.findById(id).ifPresent(brand -> {
            brand.setDeleted(true);
            brandRepository.save(brand);
        });
    }

    public void deleteBrand(Long id, com.pbl3.project.pbl3_project.entity.User actor) {
        authorizationService.requireMasterDataAccess(actor);
        deleteBrand(id);
    }
}
