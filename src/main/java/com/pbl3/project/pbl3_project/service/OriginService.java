package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.Origin;
import com.pbl3.project.pbl3_project.repository.OriginRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OriginService {
    private final OriginRepository originRepository;

    public OriginService(OriginRepository originRepository) {
        this.originRepository = originRepository;
    }

    public List<Origin> getAllOrigins() {
        return originRepository.findAllByIsDeletedFalse();
    }

    public Origin saveOrigin(Origin origin) {
        return originRepository.save(origin);
    }

    public void deleteOrigin(Long id) {
        originRepository.findById(id).ifPresent(origin -> {
            origin.setDeleted(true);
            originRepository.save(origin);
        });
    }
}
