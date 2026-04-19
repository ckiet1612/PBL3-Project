package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    long countByRoleAndEnabledTrue(com.pbl3.project.pbl3_project.entity.Role role);
}
