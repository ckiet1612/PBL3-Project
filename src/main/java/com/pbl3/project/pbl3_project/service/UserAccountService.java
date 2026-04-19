package com.pbl3.project.pbl3_project.service;

import com.pbl3.project.pbl3_project.entity.AccountAuditAction;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Set;

@Service
public class UserAccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthorizationService authorizationService;
    private final AccountAuditLogService accountAuditLogService;

    public UserAccountService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        AuthorizationService authorizationService,
        AccountAuditLogService accountAuditLogService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authorizationService = authorizationService;
        this.accountAuditLogService = accountAuditLogService;
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsers(User actor, String search, Set<Role> roles, Boolean enabled, Pageable pageable) {
        authorizationService.requireAccountsAccess(actor);
        Pageable sanitizedPageable = PageSortSupport.sanitize(
            pageable,
            Sort.by(Sort.Direction.ASC, "username"),
            java.util.Set.of("username", "fullName", "role")
        );
        Specification<User> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            String normalizedSearch = search == null ? null : search.trim().toLowerCase();
            if (normalizedSearch != null && !normalizedSearch.isEmpty()) {
                String likeValue = "%" + normalizedSearch + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(cb.coalesce(root.get("username"), "")), likeValue),
                    cb.like(cb.lower(cb.coalesce(root.get("fullName"), "")), likeValue)
                ));
            }
            if (roles != null && !roles.isEmpty()) {
                predicates.add(root.get("role").in(roles));
            }
            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return userRepository.findAll(spec, sanitizedPageable);
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsers(String search, Set<Role> roles, Boolean enabled, Pageable pageable) {
        return searchUsers(null, search, roles, enabled, pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public User createUser(User actor, String username, String rawPassword, String fullName, Role role) {
        authorizationService.requireAccountsAccess(actor);
        validateUsername(username, null);
        validatePassword(rawPassword);
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new RuntimeException("Full name is required");
        }
        if (role == null) {
            throw new RuntimeException("Role is required");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName.trim());
        user.setRole(role);
        user.setEnabled(true);
        User saved = userRepository.save(user);
        accountAuditLogService.record(actor, saved, AccountAuditAction.CREATE_ACCOUNT, "Created account with role " + role);
        return saved;
    }

    @Transactional
    public User updateUserProfile(User actor, Long targetUserId, String username, String fullName) {
        authorizationService.requireAccountsAccess(actor);
        User target = getUserById(targetUserId);
        validateUsername(username, target.getId());
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new RuntimeException("Full name is required");
        }
        target.setUsername(username.trim());
        target.setFullName(fullName.trim());
        User saved = userRepository.save(target);
        accountAuditLogService.record(actor, saved, AccountAuditAction.UPDATE_PROFILE, "Updated account profile");
        return saved;
    }

    @Transactional
    public User changeUserRole(User actor, Long targetUserId, Role newRole) {
        authorizationService.requireAccountsAccess(actor);
        if (newRole == null) {
            throw new RuntimeException("Role is required");
        }
        User target = getUserById(targetUserId);
        validateSelfRoleChange(actor, target, newRole);
        validateAdminRetention(target, target.isEnabled(), newRole);
        target.setRole(newRole);
        User saved = userRepository.save(target);
        accountAuditLogService.record(actor, saved, AccountAuditAction.CHANGE_ROLE, "Changed role to " + newRole);
        return saved;
    }

    @Transactional
    public User setUserEnabled(User actor, Long targetUserId, boolean enabled) {
        authorizationService.requireAccountsAccess(actor);
        User target = getUserById(targetUserId);
        validateSelfDisable(actor, target, enabled);
        validateAdminRetention(target, enabled, target.getRole());
        target.setEnabled(enabled);
        User saved = userRepository.save(target);
        accountAuditLogService.record(
            actor,
            saved,
            enabled ? AccountAuditAction.ENABLE_ACCOUNT : AccountAuditAction.DISABLE_ACCOUNT,
            enabled ? "Enabled account" : "Disabled account"
        );
        return saved;
    }

    @Transactional
    public User resetUserPassword(User actor, Long targetUserId, String newPassword) {
        authorizationService.requireAccountsAccess(actor);
        validatePassword(newPassword);
        User target = getUserById(targetUserId);
        target.setPassword(passwordEncoder.encode(newPassword));
        User saved = userRepository.save(target);
        accountAuditLogService.record(actor, saved, AccountAuditAction.RESET_PASSWORD, "Reset password");
        return saved;
    }

    @Transactional
    public User changeOwnPassword(User actor, String currentPassword, String newPassword, String confirmPassword) {
        authorizationService.requireSalesAccess(actor);
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new RuntimeException("Current password is required");
        }
        validatePassword(newPassword);
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Password confirmation does not match");
        }

        User managedActor = getUserById(actor.getId());
        if (!matchesPassword(currentPassword, managedActor.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        managedActor.setPassword(passwordEncoder.encode(newPassword));
        User saved = userRepository.save(managedActor);
        accountAuditLogService.record(saved, saved, AccountAuditAction.CHANGE_OWN_PASSWORD, "Changed own password");
        return saved;
    }

    private void validateUsername(String username, Long ignoreUserId) {
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        userRepository.findByUsernameIgnoreCase(username.trim())
            .filter(existing -> ignoreUserId == null || !existing.getId().equals(ignoreUserId))
            .ifPresent(existing -> { throw new RuntimeException("Username already exists"); });
    }

    private void validatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 4) {
            throw new RuntimeException("Password must be at least 4 characters");
        }
    }

    private boolean matchesPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    private void validateSelfDisable(User actor, User target, boolean enabled) {
        if (!enabled && actor != null && actor.getId() != null && actor.getId().equals(target.getId())) {
            throw new RuntimeException("You cannot disable your own account");
        }
    }

    private void validateSelfRoleChange(User actor, User target, Role newRole) {
        if (actor == null || actor.getId() == null || !actor.getId().equals(target.getId())) {
            return;
        }
        if (actor.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            long enabledAdmins = userRepository.countByRoleAndEnabledTrue(Role.ADMIN);
            if (enabledAdmins <= 1) {
                throw new RuntimeException("You cannot demote the last enabled ADMIN");
            }
        }
    }

    private void validateAdminRetention(User target, boolean enabled, Role resultingRole) {
        boolean wasEnabledAdmin = target.isEnabled() && target.getRole() == Role.ADMIN;
        boolean remainsEnabledAdmin = enabled && resultingRole == Role.ADMIN;
        if (!wasEnabledAdmin || remainsEnabledAdmin) {
            return;
        }
        long enabledAdmins = userRepository.countByRoleAndEnabledTrue(Role.ADMIN);
        if (enabledAdmins <= 1) {
            throw new RuntimeException("The system must keep at least one enabled ADMIN");
        }
    }
}
