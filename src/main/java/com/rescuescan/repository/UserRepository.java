package com.rescuescan.repository;

import com.rescuescan.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByQrCode(String qrCode);
    boolean existsByUsername(String username);
}
