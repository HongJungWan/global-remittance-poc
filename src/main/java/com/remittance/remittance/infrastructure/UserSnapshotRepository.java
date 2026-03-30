package com.remittance.remittance.infrastructure;

import com.remittance.remittance.domain.UserSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserSnapshotRepository extends JpaRepository<UserSnapshot, UUID> {
}
