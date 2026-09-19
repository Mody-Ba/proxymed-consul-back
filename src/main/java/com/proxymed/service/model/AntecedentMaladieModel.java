package com.proxymed.service.model;

import lombok.Builder;

import java.util.UUID;

/**
 * Couche interne neutre : ni le contrat API (AntecedentMaladieRequest/Response), ni l'entite JPA.
 * maladieChroniqueLibelle et consultationId ne sont renseignes que sur le chemin de lecture
 * (Entity -> Model), le second pour permettre au service de verifier l'appartenance
 * a la consultation sans relire l'entite.
 */
@Builder(toBuilder = true)
public record AntecedentMaladieModel(
        Long id,
        UUID consultationId,
        Long maladieChroniqueId,
        String maladieChroniqueLibelle,
        String precision
) {
}
