package com.proxymed.enums;

/**
 * Statut d'une fiche scannee importee : EN_ATTENTE du visa d'un senior, VISEE, ou RENVOYEE
 * avec un motif (elle pourra alors etre reimportee).
 */
public enum StatutFicheSignee {
    EN_ATTENTE,
    VISEE,
    RENVOYEE
}
