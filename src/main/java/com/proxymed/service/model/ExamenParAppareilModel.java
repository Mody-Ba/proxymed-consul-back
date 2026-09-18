package com.proxymed.service.model;

import lombok.Builder;

/**
 * Couche interne neutre : ni le contrat API (ExamenParAppareilRequest/Response), ni l'entite JPA.
 */
@Builder(toBuilder = true)
public record ExamenParAppareilModel(
        Long id,
        String etatGeneral,
        String cardioVasculaire,
        String respiratoire,
        String digestif,
        String neurologique,
        String locomoteurCutaneAutre
) {
}
