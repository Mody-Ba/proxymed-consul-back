package com.proxymed.repository;

import com.proxymed.entity.FicheSignee;
import com.proxymed.enums.StatutFicheSignee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    /**
     * Fiches d'un statut, pour la liste du senior, sans leur contenu. Le tri vient du Pageable.
     */
    @Query(value = """
            select f.id as ficheId, c.id as visiteId, p.id as patientId, p.nomComplet as nomPatient,
                   p.numeroDossierProxymed as numeroDossierProxymed, c.typeVisite as typeVisite,
                   c.dateConsultation as dateConsultation, f.dateImport as dateImport, f.statut as statut,
                   f.nomSeniorVisa as nomSeniorVisa, f.dateVisa as dateVisa, f.motifRenvoi as motifRenvoi
            from FicheSignee f join f.consultation c join c.patient p
            where f.statut = :statut
            """,
            countQuery = "select count(f) from FicheSignee f where f.statut = :statut")
    Page<FicheAViserResume> findAViserByStatut(StatutFicheSignee statut, Pageable pageable);

    /** Statut et version de la fiche de la visite, sans charger son contenu (avant un visa ou un renvoi). */
    @Query("""
            select f.id as id, f.statut as statut, f.version as version
            from FicheSignee f where f.consultation.id = :visiteId
            """)
    Optional<EtatFicheSignee> findEtatByConsultationId(UUID visiteId);

    /**
     * Visa sous verrou optimiste : ne modifie la fiche que si elle est toujours EN_ATTENTE et a la
     * version lue juste avant. Une requete de mise a jour ne passe pas par @Version : la version
     * est donc incrementee ici, ce qui fait aussi echouer un reimport concurrent de l'entite.
     *
     * @return 1 si la fiche a ete visee, 0 si elle a change entre-temps
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update FicheSignee f
            set f.statut = com.proxymed.enums.StatutFicheSignee.VISEE, f.seniorVisaId = :seniorId,
                f.nomSeniorVisa = :nomSenior, f.dateVisa = :dateVisa, f.version = f.version + 1
            where f.id = :ficheId and f.version = :version
              and f.statut = com.proxymed.enums.StatutFicheSignee.EN_ATTENTE
            """)
    int viserSiVersion(UUID ficheId, long version, UUID seniorId, String nomSenior, Instant dateVisa);

    /**
     * Renvoi sous verrou optimiste, comme viserSiVersion.
     *
     * @return 1 si la fiche a ete renvoyee, 0 si elle a change entre-temps
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update FicheSignee f
            set f.statut = com.proxymed.enums.StatutFicheSignee.RENVOYEE, f.motifRenvoi = :motif,
                f.version = f.version + 1
            where f.id = :ficheId and f.version = :version
              and f.statut = com.proxymed.enums.StatutFicheSignee.EN_ATTENTE
            """)
    int renvoyerSiVersion(UUID ficheId, long version, String motif);
}
