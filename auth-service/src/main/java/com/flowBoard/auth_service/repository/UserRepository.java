package com.flowBoard.auth_service.repository;

import com.flowBoard.auth_service.entity.ROLE;
import com.flowBoard.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<User> findAllByRole(ROLE role);

    @Query("SELECT u FROM User u WHERE "+
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :key, '%')) OR"+
            " LOWER(u.username) LIKE LOWER(CONCAT('%', :key, '%'))")
    List<User> searchByNameOrUsername(@Param("key") String key);

    void deleteById(Long id);
}