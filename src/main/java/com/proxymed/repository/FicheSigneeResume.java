package com.proxymed.repository;

import com.proxymed.enums.StatutFicheSignee;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Projection des metadonnees d'une fiche signee, sans son contenu : lister le dossier d'un
 * patient ne doit pas charger tous ses PDF (jusqu'a 10 Mo chacun) en memoire.
 */
public interface FicheSigneeResume {

    UUID getId();

    UUID getConsultationId();

    UUID getPatientId();

    LocalDate getDateConsultation();

    String getNomFichier();

    String getTypeContenu();

    long getTailleOctets();

    Instant getDateImport();

    StatutFicheSignee getStatut();

    UUID getSeniorVisaId();

    String getNomSeniorVisa();

    Instant getDateVisa();

    String getMotifRenvoi();
}
