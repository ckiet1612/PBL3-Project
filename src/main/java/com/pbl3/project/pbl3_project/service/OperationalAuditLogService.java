package com.pbl3.project.pbl3_project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalAuditLog;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.OperationalAuditLogRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class OperationalAuditLogService {

    private final OperationalAuditLogRepository repository;
    private final AuthorizationService authorizationService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public OperationalAuditLogService(OperationalAuditLogRepository repository, AuthorizationService authorizationService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
    }

    public OperationalAuditLog record(
        User actor,
        OperationalAuditAction action,
        OperationalSubjectType subjectType,
        Long subjectId,
        String subjectLabel,
        String details
    ) {
        OperationalAuditLog log = new OperationalAuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setSubjectType(subjectType);
        log.setSubjectId(subjectId);
        log.setSubjectLabel(subjectLabel);
        log.setDetails(details);
        return repository.save(log);
    }

    public OperationalAuditLog recordChange(
        User actor,
        OperationalAuditAction action,
        OperationalSubjectType subjectType,
        Long subjectId,
        String subjectLabel,
        String details,
        Object beforeState,
        Object afterState
    ) {
        OperationalAuditLog log = new OperationalAuditLog();
        log.setActor(actor);
        log.setAction(action);
        log.setSubjectType(subjectType);
        log.setSubjectId(subjectId);
        log.setSubjectLabel(subjectLabel);
        log.setDetails(details);
        log.setBeforeJson(toJson(beforeState));
        log.setAfterJson(toJson(afterState));
        return repository.save(log);
    }

    public Page<OperationalAuditLog> searchOperationalAuditLogs(
        User viewer,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<String> actorUsernames,
        Set<OperationalAuditAction> actions,
        Set<OperationalSubjectType> subjectTypes,
        Pageable pageable
    ) {
        if (viewer != null) {
            authorizationService.requireAuditLogAccess(viewer);
        }
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.DESC, "createdAt"),
            Set.of("createdAt", "actor.username", "action", "subjectType", "subjectLabel")
        );
        Specification<OperationalAuditLog> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var actorJoin = root.join("actor", JoinType.LEFT);

            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(actorJoin.get("username"), "")), likeValue),
                    cb.like(cb.lower(root.get("action").as(String.class)), likeValue),
                    cb.like(cb.lower(root.get("subjectType").as(String.class)), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("subjectLabel"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("details"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("beforeJson"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("afterJson"), "")), likeValue)
                ));
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
            }
            if (actorUsernames != null && !actorUsernames.isEmpty()) {
                predicates.add(actorJoin.get("username").in(actorUsernames));
            }
            if (actions != null && !actions.isEmpty()) {
                predicates.add(root.get("action").in(actions));
            }
            if (subjectTypes != null && !subjectTypes.isEmpty()) {
                predicates.add(root.get("subjectType").in(subjectTypes));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return repository.findAll(spec, sanitizedPageable);
    }

    public List<String> getActorUsernames(User viewer) {
        if (viewer != null) {
            authorizationService.requireAuditLogAccess(viewer);
        }
        return new LinkedHashSet<>(repository.findDistinctActorUsernames()).stream().toList();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"snapshotError\":\"" + escapeJson(ex.getMessage()) + "\"}";
        }
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}
