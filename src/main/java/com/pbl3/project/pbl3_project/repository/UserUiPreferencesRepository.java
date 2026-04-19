package com.pbl3.project.pbl3_project.repository;

import com.pbl3.project.pbl3_project.entity.UserUiPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserUiPreferencesRepository extends JpaRepository<UserUiPreferences, Long> {
    Optional<UserUiPreferences> findByUserId(Long userId);
}
