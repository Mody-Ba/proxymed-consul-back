package com.proxymed.service.model;

import com.proxymed.enums.FrequenceVisites;

import java.util.UUID;

/**
 * Couche interne neutre : une fiche a viser, avec la frequence des visites decidee par le senior
 * (null si non precisee).
 */
public record VisaFicheModel(UUID visiteId, FrequenceVisites frequenceVisites) {
}
