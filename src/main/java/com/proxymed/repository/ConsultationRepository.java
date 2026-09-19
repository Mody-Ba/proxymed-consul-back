package com.proxymed.repository;

import com.proxymed.entity.ConsultationInitiale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * JpaSpecificationExecutor permet de construire les filtres de recherche
 * (nom, n° dossier, medecin senior, statut, decision, structure - section 5.4)
 * sans multiplier les methodes derivees.
 */
@Repository
public interface ConsultationRepository extends JpaRepository<ConsultationInitiale, UUID>,
        JpaSpecificationExecutor<ConsultationInitiale> {
}
