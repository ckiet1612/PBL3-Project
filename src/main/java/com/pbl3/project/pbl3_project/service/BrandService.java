package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Brand;
import com.pbl3.project.pbl3_project.repository.BrandRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BrandService {
    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public List<Brand> getAllBrands() {
        return brandRepository.findAllByIsDeletedFalse();
    }

    public Brand saveBrand(Brand brand) {
        return brandRepository.save(brand);
    }

    public void deleteBrand(Long id) {
        brandRepository.findById(id).ifPresent(brand -> {
            brand.setDeleted(true);
            brandRepository.save(brand);
        });
    }
}
