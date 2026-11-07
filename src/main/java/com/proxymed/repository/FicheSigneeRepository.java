package com.proxymed.repository;

import com.proxymed.entity.FicheSignee;
import com.proxymed.enums.StatutFicheSignee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FicheSigneeRepository extends JpaRepository<FicheSignee, UUID> {

    /** Fiche signee de la consultation (au plus une, contrainte uk_fiche_signee_consultation). */
    Optional<FicheSignee> findByConsultation_Id(UUID consultationId);

    boolean existsByConsultation_Id(UUID consultationId);

    /** Statut de la fiche de la consultation, sans charger son contenu. */
    @Query("select f.statut from FicheSignee f where f.consultation.id = :consultationId")
    Optional<StatutFicheSignee> findStatutByConsultationId(UUID consultationId);

    /** Fiches signees du patient, de la plus recemment importee a la plus ancienne, sans leur contenu. */
    @Query("""
            select f.id as id, c.id as consultationId, c.patient.id as patientId,
                   c.dateConsultation as dateConsultation, f.nomFichier as nomFichier,
                   f.typeContenu as typeContenu, f.tailleOctets as tailleOctets, f.dateImport as dateImport,
                   f.statut as statut, f.seniorVisaId as seniorVisaId, f.nomSeniorVisa as nomSeniorVisa,
                   f.dateVisa as dateVisa, f.motifRenvoi as motifRenvoi
            from FicheSignee f join f.consultation c
            where c.patient.id = :patientId
            order by f.dateImport desc
            """)
    List<FicheSigneeResume> findResumesByPatientId(UUID patientId);
}
