package com.proxymed.repository;

import com.proxymed.entity.DemandeConsultation;
import com.proxymed.enums.StatutDemandeConsultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DemandeConsultationRepository extends JpaRepository<DemandeConsultation, UUID> {

    Page<DemandeConsultation> findByDemandeurId(UUID demandeurId, Pageable pageable);

    Page<DemandeConsultation> findByStatut(StatutDemandeConsultation statut, Pageable pageable);

    Page<DemandeConsultation> findByJuniorIdAndStatut(UUID juniorId, StatutDemandeConsultation statut, Pageable pageable);
}
