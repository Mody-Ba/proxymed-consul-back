package com.proxymed.repository;

import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.enums.TypeVisite;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Ligne de la liste des fiches du senior, sans le contenu du PDF : de quoi reconnaitre la fiche
 * (patient, type de visite, dates) et, selon son statut, le visa ou le motif du renvoi.
 */
public interface FicheAViserResume {

    UUID getFicheId();

    UUID getVisiteId();

    UUID getPatientId();

    String getNomPatient();

    String getNumeroDossierProxymed();

    TypeVisite getTypeVisite();

    LocalDate getDateConsultation();

    Instant getDateImport();

    StatutFicheSignee getStatut();

    String getNomSeniorVisa();

    Instant getDateVisa();

    String getMotifRenvoi();
}
