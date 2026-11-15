package com.proxymed.service.model;

import com.proxymed.enums.FrequenceVisites;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.TypeVisite;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Couche interne neutre : de quoi produire le rapport d'une fiche VISEE. La page de garde ne porte
 * que l'identite du patient, la visite et le visa (dont la frequence des visites decidee par le senior) ; scan est le PDF signe, joint tel quel (aucune
 * donnee medicale n'en est extraite).
 */
@Builder(toBuilder = true)
public record RapportFicheModel(
        UUID consultationId,
        String nomPatient,
        String numeroDossierProxymed,
        String numeroDmi,
        LocalDate dateNaissance,
        Sexe sexe,
        TypeVisite typeVisite,
        LocalDate dateVisite,
        String nomSeniorVisa,
        Instant dateVisa,
        FrequenceVisites frequenceVisitesSenior,
        byte[] scan
) {
}
