package com.remittance.remittance.infrastructure;

import com.remittance.remittance.domain.RemittanceOrder;
import com.remittance.remittance.domain.vo.RemittanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RemittanceOrderRepository extends JpaRepository<RemittanceOrder, UUID> {

    List<RemittanceOrder> findBySenderIdOrderByCreatedAtDesc(UUID senderId);

    List<RemittanceOrder> findByStatus(RemittanceStatus status);

    List<RemittanceOrder> findByStatusAndUpdatedAtBefore(RemittanceStatus status, Instant cutoff);
}
