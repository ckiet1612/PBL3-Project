package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.AccountAuditAction;
import com.pbl3.project.pbl3_project.entity.AccountAuditLog;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.AccountAuditLogRepository;
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
public class AccountAuditLogService {

    private final AccountAuditLogRepository accountAuditLogRepository;
    private final AuthorizationService authorizationService;

    public AccountAuditLogService(AccountAuditLogRepository accountAuditLogRepository, AuthorizationService authorizationService) {
        this.accountAuditLogRepository = accountAuditLogRepository;
        this.authorizationService = authorizationService;
    }

    public AccountAuditLog record(User actor, User targetUser, AccountAuditAction action, String details) {
        AccountAuditLog log = new AccountAuditLog();
        log.setActor(actor);
        log.setTargetUser(targetUser);
        log.setAction(action);
        log.setDetails(details);
        return accountAuditLogRepository.save(log);
    }

    public Page<AccountAuditLog> searchAccountAuditLogs(
        User viewer,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<String> actorUsernames,
        Set<String> targetUsernames,
        Set<AccountAuditAction> actions,
        Pageable pageable
    ) {
        if (viewer != null) {
            authorizationService.requireAccountsAccess(viewer);
        }
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.DESC, "createdAt"),
            java.util.Set.of("createdAt", "actor.username", "targetUser.username", "action")
        );
        Specification<AccountAuditLog> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var actorJoin = root.join("actor", JoinType.LEFT);
            var targetJoin = root.join("targetUser", JoinType.LEFT);

            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(actorJoin.get("username"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(targetJoin.get("username"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(targetJoin.get("fullName"), "")), likeValue),
                    cb.like(cb.lower(root.get("action").as(String.class)), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("details"), "")), likeValue)
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
            if (targetUsernames != null && !targetUsernames.isEmpty()) {
                predicates.add(targetJoin.get("username").in(targetUsernames));
            }
            if (actions != null && !actions.isEmpty()) {
                predicates.add(root.get("action").in(actions));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return accountAuditLogRepository.findAll(spec, sanitizedPageable);
    }

    public Page<AccountAuditLog> searchAccountAuditLogs(
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<String> actorUsernames,
        Set<String> targetUsernames,
        Set<AccountAuditAction> actions,
        Pageable pageable
    ) {
        return searchAccountAuditLogs(null, search, startDate, endDate, actorUsernames, targetUsernames, actions, pageable);
    }

    public List<String> getActorUsernames(User viewer) {
        if (viewer != null) {
            authorizationService.requireAccountsAccess(viewer);
        }
        return new LinkedHashSet<>(accountAuditLogRepository.findDistinctActorUsernames()).stream().toList();
    }

    public List<String> getActorUsernames() {
        return new LinkedHashSet<>(accountAuditLogRepository.findDistinctActorUsernames()).stream().toList();
    }

    public List<String> getTargetUsernames(User viewer) {
        if (viewer != null) {
            authorizationService.requireAccountsAccess(viewer);
        }
        return new LinkedHashSet<>(accountAuditLogRepository.findDistinctTargetUsernames()).stream().toList();
    }

    public List<String> getTargetUsernames() {
        return new LinkedHashSet<>(accountAuditLogRepository.findDistinctTargetUsernames()).stream().toList();
    }
}
