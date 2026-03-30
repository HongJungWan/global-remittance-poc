package com.remittance.user.infrastructure;

import com.remittance.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailValue(String email);

    boolean existsByEmailValue(String email);
}
