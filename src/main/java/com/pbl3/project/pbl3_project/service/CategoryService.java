package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Category;
import com.pbl3.project.pbl3_project.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final AuthorizationService authorizationService;

    public CategoryService(CategoryRepository categoryRepository, AuthorizationService authorizationService) {
        this.categoryRepository = categoryRepository;
        this.authorizationService = authorizationService;
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Page<Category> searchCategories(String search, Pageable pageable) {
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.ASC, "name"),
            java.util.Set.of("name")
        );
        return categoryRepository.findAll(
            (root, query, cb) -> {
                if (search != null && !search.trim().isEmpty()) {
                    return cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%");
                }
                return cb.conjunction();
            },
            sanitizedPageable
        );
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    public Category saveCategory(Category category, com.pbl3.project.pbl3_project.entity.User actor) {
        authorizationService.requireMasterDataAccess(actor);
        return saveCategory(category);
    }
    
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }

    public void deleteCategory(Long id, com.pbl3.project.pbl3_project.entity.User actor) {
        authorizationService.requireMasterDataAccess(actor);
        deleteCategory(id);
    }
}
