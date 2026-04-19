package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Origin;
import com.pbl3.project.pbl3_project.repository.OriginRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OriginService {
    private final OriginRepository originRepository;
    private final AuthorizationService authorizationService;

    public OriginService(OriginRepository originRepository, AuthorizationService authorizationService) {
        this.originRepository = originRepository;
        this.authorizationService = authorizationService;
    }

    public List<Origin> getAllOrigins() {
        return originRepository.findAllByIsDeletedFalse();
    }

    public Page<Origin> searchOrigins(String search, Pageable pageable) {
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.ASC, "name"),
            java.util.Set.of("name")
        );
        return originRepository.findAll(
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

    public Origin saveOrigin(Origin origin) {
        return originRepository.save(origin);
    }

    public Origin saveOrigin(Origin origin, com.pbl3.project.pbl3_project.entity.User actor) {
        authorizationService.requireMasterDataAccess(actor);
        return saveOrigin(origin);
    }

    public void deleteOrigin(Long id) {
        originRepository.findById(id).ifPresent(origin -> {
            origin.setDeleted(true);
            originRepository.save(origin);
        });
    }

    public void deleteOrigin(Long id, com.pbl3.project.pbl3_project.entity.User actor) {
        authorizationService.requireMasterDataAccess(actor);
        deleteOrigin(id);
    }
}
