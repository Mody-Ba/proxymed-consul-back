package com.proxymed.repository;

import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.enums.TypeVisite;

import java.util.UUID;

/**
 * Projection minimale d'une fiche signee pour un visa ou un renvoi : statut et version (verrou
 * optimiste), et type de la visite (frequence acceptee seulement pour une INITIALE), sans le contenu du PDF.
 */
public interface EtatFicheSignee {

    UUID getId();

    StatutFicheSignee getStatut();

    Long getVersion();

    TypeVisite getTypeVisite();
}
