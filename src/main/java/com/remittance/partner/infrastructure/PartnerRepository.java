package com.remittance.partner.infrastructure;

import com.remittance.partner.domain.PartnerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<PartnerTransaction, UUID> {

    Optional<PartnerTransaction> findByRemittanceId(UUID remittanceId);
}
