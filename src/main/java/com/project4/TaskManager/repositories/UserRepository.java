package com.project4.TaskManager.repositories;

import com.project4.TaskManager.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByIsDeletedFalse();
}